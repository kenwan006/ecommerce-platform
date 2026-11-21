# Sunridge AWS CDK foundation

This CDK application creates only the first AWS layer for Sunridge:

- a VPC across two Availability Zones;
- public application and isolated database subnets;
- an application security group (no inbound access); and
- an RDS MySQL instance for Commerce, Warehouse, Payment, and Notification;
- an RDS PostgreSQL instance for the FastAPI agent; and
- generated RDS credentials stored in AWS Secrets Manager; and
- six private ECR repositories for service images.

The second stack creates one Amazon Linux 2023 EC2 application host. It is a
`t4g.large` (ARM64) with a 20 GiB encrypted root disk, Docker installed, no SSH key,
and no inbound network rules. Administer it using AWS Systems Manager Session
Manager. It can read only the two database bootstrap secrets.

It deliberately does **not** create a load balancer, DNS, Kafka, or deploy application containers. Those resources are reviewed separately before they receive traffic.

## Prerequisites

Install Node.js 20+, AWS CLI v2, and authenticate to the intended AWS account. IAM Identity Center is preferred:

```bash
aws configure sso --profile sunridge-dev
aws sso login --profile sunridge-dev
aws sts get-caller-identity --profile sunridge-dev
```

AWS IAM Identity Center credentials are temporary. When a CDK or AWS CLI command
reports expired credentials, refresh the session and rerun the command:

```bash
aws sso login --profile sunridge-dev
```

Install the CDK project dependencies:

```bash
npm install
```

Bootstrap each AWS account/region once. This project currently deploys to US East (Ohio), `us-east-2`:

```bash
npx aws-cdk bootstrap aws://919537457365/us-east-2 --profile sunridge-dev
```

## Review before deployment

```bash
npm run build
npx aws-cdk synth --profile sunridge-dev --region us-east-2
npx aws-cdk diff --profile sunridge-dev --region us-east-2
```

The default context is a cost-conscious development setup with single-AZ database instances. Deploy it only after reviewing the generated CloudFormation:

```bash
npx aws-cdk deploy --profile sunridge-dev --region us-east-2
```

For a production environment, use a separate AWS account or at least a separate CDK environment and enable deletion protection:

```bash
npx aws-cdk deploy \
  -c environmentName=prod \
  -c deletionProtection=true \
  --profile sunridge-dev \
  --region us-east-2
```

## Database ownership and initialization

The MySQL instance is intentionally one instance, not one shared schema. After it is deployed, create four logical databases and application-specific, least-privilege users:

```text
sunridge_commerce
sunridge_warehouse
sunridge_payment
sunridge_notification
```

The PostgreSQL instance creates `sunridge_agent`. The agent's existing `agent-service/db/001_rag.sql` enables the `vector` extension and must be applied after deployment. The selected PostgreSQL 18.3 engine supports pgvector on RDS; verify extension support again if the engine version changes. See [AWS's supported-extension list](https://docs.aws.amazon.com/AmazonRDS/latest/PostgreSQLReleaseNotes/postgresql-extensions.html).

The CDK-generated database secrets are administrative bootstrap credentials. Do not put them in source control or inject them into all applications. A later application stack should create separate service secrets and grant each service only its database.

## Cost and network notes

This initial learning environment intentionally has **no NAT gateway**, avoiding its hourly and data-processing charges. Future EC2 compute will use a public application subnet so it can pull images and receive web traffic. Configure its security group to accept only HTTPS (and HTTP only for redirecting to HTTPS); manage it through SSM rather than opening SSH.

Both databases are private: they accept connections only from the `ApplicationSecurityGroup` created by this stack. Do not add public database access.

RDS resources use snapshot retention on stack deletion and their generated secrets are retained. This reduces accidental data loss, but means `cdk destroy` will not fully remove retained data/secrets.
