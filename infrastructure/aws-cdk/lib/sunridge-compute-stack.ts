import * as cdk from 'aws-cdk-lib';
import { CfnOutput, Stack, StackProps } from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { SunridgeApplicationSecretsStack } from './sunridge-application-secrets-stack';
import { SunridgeDatabaseStack } from './sunridge-database-stack';

export interface SunridgeComputeStackProps extends StackProps {
  readonly environmentName: string;
  readonly databaseStack: SunridgeDatabaseStack;
  readonly applicationSecretsStack: SunridgeApplicationSecretsStack;
}

/**
 * Initial single-host application environment. It has no inbound network
 * access yet: administration uses AWS Systems Manager Session Manager, and a
 * later stack will add an ALB and HTTPS listener before browser traffic is
 * accepted.
 */
export class SunridgeComputeStack extends Stack {
  public constructor(scope: Construct, id: string, props: SunridgeComputeStackProps) {
    super(scope, id, props);

    const resourcePrefix = `sunridge-${props.environmentName}`;
    const instanceRole = new iam.Role(this, 'InstanceRole', {
      roleName: `${resourcePrefix}-ec2`,
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      description: 'Allows the Sunridge EC2 host to use Session Manager and read database bootstrap secrets.',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEC2ContainerRegistryReadOnly'),
      ],
    });
    props.databaseStack.mysqlAdminSecret.grantRead(instanceRole);
    props.databaseStack.postgresAgentSecret.grantRead(instanceRole);
    props.applicationSecretsStack.mysqlApplicationSecret.grantRead(instanceRole);
    props.applicationSecretsStack.jwtSecret.grantRead(instanceRole);
    props.applicationSecretsStack.internalApiKeySecret.grantRead(instanceRole);
    props.applicationSecretsStack.externalConfigSecret.grantRead(instanceRole);

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      'dnf install -y docker git curl',
      'systemctl enable --now docker',
      'mkdir -p /usr/local/lib/docker/cli-plugins',
      'curl -fsSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64 -o /usr/local/lib/docker/cli-plugins/docker-compose',
      'chmod +x /usr/local/lib/docker/cli-plugins/docker-compose',
      'usermod -aG docker ec2-user',
    );

    const instance = new ec2.Instance(this, 'ApplicationHost', {
      instanceName: `${resourcePrefix}-app`,
      vpc: props.databaseStack.vpc,
      vpcSubnets: { subnetGroupName: 'application-public' },
      securityGroup: props.databaseStack.applicationSecurityGroup,
      role: instanceRole,
      // Smallest reasonable single-host size for the four JVM services, the
      // FastAPI agent, and Redpanda. Scale services out before production.
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.LARGE),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64,
      }),
      associatePublicIpAddress: true,
      requireImdsv2: true,
      userData,
      blockDevices: [{
        deviceName: '/dev/xvda',
        volume: ec2.BlockDeviceVolume.ebs(20, {
          deleteOnTermination: true,
          encrypted: true,
          volumeType: ec2.EbsDeviceVolumeType.GP3,
        }),
      }],
    });

    new CfnOutput(this, 'ApplicationHostInstanceId', {
      value: instance.instanceId,
      description: 'Use this ID with aws ssm start-session to administer the host.',
    });
    new CfnOutput(this, 'ApplicationHostPrivateIp', { value: instance.instancePrivateIp });
    new CfnOutput(this, 'SsmStartSessionCommand', {
      value: `aws ssm start-session --target ${instance.instanceId}`,
      description: 'Run locally with --profile sunridge-dev and --region us-east-2.',
    });

    cdk.Tags.of(this).add('Application', 'sunridge-commerce');
    cdk.Tags.of(this).add('Environment', props.environmentName);
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }
}
