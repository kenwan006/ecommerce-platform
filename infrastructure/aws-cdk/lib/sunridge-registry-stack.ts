import * as cdk from 'aws-cdk-lib';
import { CfnOutput, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

export interface SunridgeRegistryStackProps extends StackProps {
  readonly environmentName: string;
}

/**
 * Private image registries for the services deployed to the single EC2 host.
 * Images are deliberately retained if this stack is removed, so an accidental
 * infrastructure delete cannot remove the currently deployable artifacts.
 */
export class SunridgeRegistryStack extends Stack {
  public constructor(scope: Construct, id: string, props: SunridgeRegistryStackProps) {
    super(scope, id, props);

    const resourcePrefix = `sunridge-${props.environmentName}`;
    const services = ['commerce', 'warehouse', 'payment', 'notification', 'agent', 'frontend'];

    for (const service of services) {
      const repository = new ecr.Repository(this, `${service[0].toUpperCase()}${service.slice(1)}Repository`, {
        repositoryName: `${resourcePrefix}/${service}`,
        imageScanOnPush: true,
        encryption: ecr.RepositoryEncryption.AES_256,
        removalPolicy: RemovalPolicy.RETAIN,
        lifecycleRules: [{ maxImageCount: 5, description: 'Keep the five most recent image versions.' }],
      });

      new CfnOutput(this, `${service[0].toUpperCase()}${service.slice(1)}RepositoryUri`, {
        value: repository.repositoryUri,
        description: `ECR repository URI for the ${service} service.`,
      });
    }

    cdk.Tags.of(this).add('Application', 'sunridge-commerce');
    cdk.Tags.of(this).add('Environment', props.environmentName);
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }
}
