import * as cdk from 'aws-cdk-lib';
import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

export interface SunridgeDatabaseStackProps extends StackProps {
  readonly environmentName: string;
  readonly deletionProtection: boolean;
}

/**
 * The first deployable AWS foundation. Application compute, ECR, a load
 * balancer, and DNS intentionally belong in later stacks.
 */
export class SunridgeDatabaseStack extends Stack {
  public readonly vpc: ec2.IVpc;
  public readonly applicationSecurityGroup: ec2.ISecurityGroup;
  public readonly mysqlAdminSecret: secretsmanager.ISecret;
  public readonly postgresAgentSecret: secretsmanager.ISecret;

  public constructor(scope: Construct, id: string, props: SunridgeDatabaseStackProps) {
    super(scope, id, props);

    const resourcePrefix = `sunridge-${props.environmentName}`;

    const vpc = new ec2.Vpc(this, 'Vpc', {
      vpcName: `${resourcePrefix}-vpc`,
      maxAzs: 2,
      // Keep the initial learning environment inexpensive. Future private
      // compute (ECS/EKS) should use private subnets plus NAT or VPC endpoints.
      natGateways: 0,
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          name: 'application-public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          name: 'database',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        },
      ],
    });

    // Later EC2/ECS/EKS workloads attach this group. It has no inbound rules.
    const applicationSecurityGroup = new ec2.SecurityGroup(this, 'ApplicationSecurityGroup', {
      vpc,
      securityGroupName: `${resourcePrefix}-application`,
      description: 'Security group for Sunridge application compute.',
      allowAllOutbound: true,
    });

    const databaseSecurityGroup = new ec2.SecurityGroup(this, 'DatabaseSecurityGroup', {
      vpc,
      securityGroupName: `${resourcePrefix}-database`,
      description: 'Permits database traffic only from Sunridge application compute.',
      allowAllOutbound: false,
    });
    databaseSecurityGroup.addIngressRule(applicationSecurityGroup, ec2.Port.tcp(3306), 'MySQL from application compute');
    databaseSecurityGroup.addIngressRule(applicationSecurityGroup, ec2.Port.tcp(5432), 'PostgreSQL from application compute');

    const mysqlSecret = new secretsmanager.Secret(this, 'MysqlAdminSecret', {
      secretName: `${resourcePrefix}/rds/mysql/admin`,
      description: 'Administrative bootstrap credentials for the Sunridge MySQL instance.',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'sunridge_admin' }),
        generateStringKey: 'password',
        excludeCharacters: ' %+~`#$&*()|[]{}:;<>?!\'/@"\\',
        passwordLength: 30,
      },
    });
    mysqlSecret.applyRemovalPolicy(RemovalPolicy.RETAIN);
    const mysql = new rds.DatabaseInstance(this, 'Mysql', {
      instanceIdentifier: `${resourcePrefix}-mysql`,
      engine: rds.DatabaseInstanceEngine.mysql({
        // Verified as the current RDS MySQL default in us-east-2.
        version: rds.MysqlEngineVersion.of('8.4.11', '8.4'),
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: { subnetGroupName: 'database' },
      securityGroups: [databaseSecurityGroup],
      credentials: rds.Credentials.fromSecret(mysqlSecret, 'sunridge_admin'),
      multiAz: false,
      publiclyAccessible: false,
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      storageEncrypted: true,
      backupRetention: Duration.days(7),
      deletionProtection: props.deletionProtection,
      removalPolicy: RemovalPolicy.SNAPSHOT,
      deleteAutomatedBackups: false,
      autoMinorVersionUpgrade: true,
      copyTagsToSnapshot: true,
    });

    const postgresSecret = new secretsmanager.Secret(this, 'PostgresAgentSecret', {
      secretName: `${resourcePrefix}/rds/postgres/agent`,
      description: 'Administrative bootstrap credentials for the Sunridge agent PostgreSQL instance.',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'sunridge_agent' }),
        generateStringKey: 'password',
        excludeCharacters: ' %+~`#$&*()|[]{}:;<>?!\'/@"\\',
        passwordLength: 30,
      },
    });
    postgresSecret.applyRemovalPolicy(RemovalPolicy.RETAIN);

    this.vpc = vpc;
    this.applicationSecurityGroup = applicationSecurityGroup;
    this.mysqlAdminSecret = mysqlSecret;
    this.postgresAgentSecret = postgresSecret;
    const postgres = new rds.DatabaseInstance(this, 'Postgres', {
      instanceIdentifier: `${resourcePrefix}-postgres`,
      engine: rds.DatabaseInstanceEngine.postgres({
        // PostgreSQL 18.3 supports the pgvector extension required by the agent.
        version: rds.PostgresEngineVersion.VER_18_3,
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: { subnetGroupName: 'database' },
      securityGroups: [databaseSecurityGroup],
      credentials: rds.Credentials.fromSecret(postgresSecret, 'sunridge_agent'),
      databaseName: 'sunridge_agent',
      multiAz: false,
      publiclyAccessible: false,
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      storageEncrypted: true,
      backupRetention: Duration.days(7),
      deletionProtection: props.deletionProtection,
      removalPolicy: RemovalPolicy.SNAPSHOT,
      deleteAutomatedBackups: false,
      autoMinorVersionUpgrade: true,
      copyTagsToSnapshot: true,
    });

    new CfnOutput(this, 'VpcId', { value: vpc.vpcId });
    new CfnOutput(this, 'ApplicationSecurityGroupId', { value: applicationSecurityGroup.securityGroupId });
    new CfnOutput(this, 'MysqlEndpointAddress', { value: mysql.dbInstanceEndpointAddress });
    new CfnOutput(this, 'MysqlAdminSecretName', { value: mysqlSecret.secretName });
    new CfnOutput(this, 'PostgresEndpointAddress', { value: postgres.dbInstanceEndpointAddress });
    new CfnOutput(this, 'PostgresAgentSecretName', { value: postgresSecret.secretName });

    cdk.Tags.of(this).add('Application', 'sunridge-commerce');
    cdk.Tags.of(this).add('Environment', props.environmentName);
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }
}
