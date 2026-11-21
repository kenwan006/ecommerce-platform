# Sunridge AWS deployment runbook

This is the command reference for the development environment in AWS account
`919537457365`, region `us-east-2`.

It deploys the current learning setup: private RDS databases, one SSM-managed
EC2 host, private ECR image repositories, and containers run with Docker
Compose. It does **not** make the application public; an ALB, HTTPS
certificate, DNS, and restricted inbound security-group rule come later.

## 1. Authenticate to AWS

Run these from any local directory. The profile contains Identity Center
configuration, not long-lived access keys. Its credentials expire, so repeat
the login command when AWS reports an expired token.

```bash
aws sso login --profile sunridge-dev

aws sts get-caller-identity --profile sunridge-dev
```

Expected account: `919537457365`.

## 2. CDK infrastructure

Run these from the CDK project directory:

```bash
cd infrastructure/aws-cdk
npm install
```

Bootstrap is required once per account and region. It creates CDK's deployment
support resources; it does not create the application databases or EC2 host.

```bash
npx aws-cdk bootstrap aws://919537457365/us-east-2 \
  --profile sunridge-dev
```

Review a local CloudFormation template without contacting AWS:

```bash
npx aws-cdk synth
```

Review the AWS-aware change set, then deploy:

```bash
CDK_DEFAULT_ACCOUNT=919537457365 CDK_DEFAULT_REGION=us-east-2 \
npx aws-cdk diff \
  --profile sunridge-dev \
  --region us-east-2

CDK_DEFAULT_ACCOUNT=919537457365 CDK_DEFAULT_REGION=us-east-2 \
npx aws-cdk deploy \
  --profile sunridge-dev \
  --region us-east-2
```

The current stacks are:

```text
SunridgeDatabase-dev  VPC, security groups, MySQL, PostgreSQL, RDS secrets
SunridgeRegistry-dev  six private ECR repositories
SunridgeCompute-dev   SSM-managed EC2 host and its IAM role
```

## 3. Connect to EC2 using SSM

No SSH key or port 22 is used.

```bash
aws ssm start-session \
  --target i-061f83e2af85bd26f \
  --profile sunridge-dev \
  --region us-east-2
```

Within the session, `whoami` should be `ssm-user`. The EC2 instance uses its
own IAM role (`sunridge-dev-ec2`) for AWS access; it does not use the local
`sunridge-dev` profile.

```bash
aws sts get-caller-identity
sudo docker --version
```

## 4. Connect to private RDS from a local database UI

Keep one terminal open per tunnel. The local port is only on your Mac; the RDS
database remains private in the VPC.

MySQL / MySQL Workbench:

```bash
aws ssm start-session \
  --target i-061f83e2af85bd26f \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["sunridge-dev-mysql.c96gmq002ust.us-east-2.rds.amazonaws.com"],"portNumber":["3306"],"localPortNumber":["13306"]}' \
  --profile sunridge-dev \
  --region us-east-2
```

PostgreSQL / pgAdmin:

```bash
aws ssm start-session \
  --target i-061f83e2af85bd26f \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["sunridge-dev-postgres.c96gmq002ust.us-east-2.rds.amazonaws.com"],"portNumber":["5432"],"localPortNumber":["15432"]}' \
  --profile sunridge-dev \
  --region us-east-2
```

Retrieve an RDS secret only when needed; do not copy it into source code:

```bash
aws secretsmanager get-secret-value \
  --secret-id sunridge-dev/rds/mysql/admin \
  --profile sunridge-dev \
  --region us-east-2 \
  --query SecretString \
  --output text
```

## 5. Build and publish Docker images to ECR

Run this on the local development machine, not on EC2. EC2 has ECR read-only
permission and pulls images; it does not build or push them.

```bash
aws ecr get-login-password \
  --region us-east-2 \
  --profile sunridge-dev \
| docker login \
  --username AWS \
  --password-stdin 919537457365.dkr.ecr.us-east-2.amazonaws.com
```

An ECR image target always has this form:

```text
<account-id>.dkr.ecr.<region>.amazonaws.com/<repository>:<tag>
```

Example: build, tag, and publish Commerce. Change `IMAGE_TAG` on each
release; the currently published complete set is `dev-20261120-0001`.

```bash
IMAGE_TAG=dev-20261120-0001
ECR_REGISTRY=919537457365.dkr.ecr.us-east-2.amazonaws.com

docker build -t sunridge-local/commerce:check backend

docker tag \
  sunridge-local/commerce:check \
  "$ECR_REGISTRY/sunridge-dev/commerce:$IMAGE_TAG"

docker push "$ECR_REGISTRY/sunridge-dev/commerce:$IMAGE_TAG"
```

The six repositories and their local build contexts are:

```text
commerce      backend
warehouse     services/warehouse-service
payment       services/payment-service
notification  services/notification-service
agent         agent-service
frontend      frontend
```

To publish all six after building them locally:

```bash
for SERVICE in commerce warehouse payment notification agent frontend; do
  docker tag "sunridge-local/${SERVICE}:check" \
    "$ECR_REGISTRY/sunridge-dev/${SERVICE}:$IMAGE_TAG"
  docker push "$ECR_REGISTRY/sunridge-dev/${SERVICE}:$IMAGE_TAG"
done
```

Verify an image after publishing:

```bash
aws ecr describe-images \
  --repository-name sunridge-dev/commerce \
  --image-ids imageTag="$IMAGE_TAG" \
  --profile sunridge-dev \
  --region us-east-2
```

## 6. Prepare runtime configuration on EC2

`compose.production.yml` expects two **untracked** files next to it:

```text
deployment/.env         non-secret deployment coordinates
deployment/runtime.env  secrets and runtime settings
```

Start from the templates:

```bash
cd ecommerce-platform/deployment
cp .env.example .env
cp runtime.env.example runtime.env
chmod 600 runtime.env
```

Fill `runtime.env` with real values from Secrets Manager or the relevant
provider. Never commit it. Required values include MySQL/PostgreSQL
credentials, `JWT_SECRET`, `INTERNAL_API_KEY`, Stripe values, and the OpenAI
and GitHub credentials used by the agent. On EC2, do not manually enter these
values: `generate-runtime-env.py` retrieves them using the EC2 IAM role.

The CDK application creates these runtime secret names when
`SunridgeApplicationSecrets-dev` is deployed:

```text
sunridge-dev/app/mysql
sunridge-dev/app/jwt
sunridge-dev/app/internal-api-key
sunridge-dev/app/external-config
```

AWS generates the MySQL password, JWT value, and internal API key. Update the
JSON in `sunridge-dev/app/external-config` in the Secrets Manager console with
your provider-owned values; do not replace generated secret values.

The agent will deliberately refuse to start without `OPENAI_API_KEY` and
`GITHUB_TOKEN`. Payment starts without Stripe credentials, but rejects payment
operations until its Stripe credentials are present.

Before starting the containers, use a non-admin MySQL application user rather
than the `sunridge_admin` bootstrap account. The RDS bootstrap secret exists
only for database administration and setup.

After the new host is available in Session Manager, provision the generated
`sunridge_app` account by running this script on the host:

```bash
bash deployment/provision-mysql-application-user.sh
```

## 7. Copy deployment files and start containers on EC2

The EC2 host runs immutable ECR images and does not need the source repository.
Copy the non-secret Compose file and runtime generator to
`/opt/sunridge/deployment`, then run the generator there. The generator writes
the protected `runtime.env` file with permissions `600`.

Install Docker Compose if it is absent (the CDK user data does this for newly
created ARM hosts):

```bash
sudo dnf install -y docker git curl --allowerasing
sudo systemctl enable --now docker
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -fsSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
sudo docker compose version
```

On EC2, Docker authenticates to ECR using the instance role:

```bash
aws ecr get-login-password --region us-east-2 \
| sudo docker login \
  --username AWS \
  --password-stdin 919537457365.dkr.ecr.us-east-2.amazonaws.com

cd /opt/sunridge/deployment
sudo python3 generate-runtime-env.py
sudo docker compose --env-file .env -f compose.production.yml config --quiet
sudo docker compose --env-file .env -f compose.production.yml pull
sudo docker compose --env-file .env -f compose.production.yml up -d
sudo docker compose --env-file .env -f compose.production.yml ps
sudo docker compose --env-file .env -f compose.production.yml logs -f --tail=100
```

Stop containers without deleting persistent Redpanda data:

```bash
sudo docker compose --env-file .env -f compose.production.yml down
```

To test the private deployment from a Mac without opening an inbound EC2 rule,
forward EC2 port 80 through Session Manager and browse to
`http://localhost:18080`:

```bash
aws ssm start-session \
  --target i-061f83e2af85bd26f \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["80"],"localPortNumber":["18080"]}' \
  --profile sunridge-dev \
  --region us-east-2
```

## 8. Current safety boundaries

- RDS has no public IP and only accepts database traffic from the application
  security group.
- EC2 is administered with Session Manager; there is no SSH inbound rule.
- EC2 can pull from ECR but cannot push images.
- Do not open port 80/443 until a direct-EC2 or ALB, TLS certificate, and
  domain strategy are reviewed.
- Identity Center sessions expire. Refresh them with `aws sso login --profile
  sunridge-dev` before AWS CLI or CDK commands.
