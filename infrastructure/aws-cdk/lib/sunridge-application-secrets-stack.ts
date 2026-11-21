import * as cdk from 'aws-cdk-lib';
import { CfnOutput, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

export interface SunridgeApplicationSecretsStackProps extends StackProps {
  readonly environmentName: string;
}

/**
 * Runtime secret records. AWS generates the credentials that belong to this
 * application; provider-owned credentials are intentionally empty until an
 * administrator supplies them through Secrets Manager.
 */
export class SunridgeApplicationSecretsStack extends Stack {
  public readonly mysqlApplicationSecret: secretsmanager.ISecret;
  public readonly jwtSecret: secretsmanager.ISecret;
  public readonly internalApiKeySecret: secretsmanager.ISecret;
  public readonly externalConfigSecret: secretsmanager.ISecret;

  public constructor(scope: Construct, id: string, props: SunridgeApplicationSecretsStackProps) {
    super(scope, id, props);

    const prefix = `sunridge-${props.environmentName}/app`;
    this.mysqlApplicationSecret = this.generatedSecret('MysqlApplicationSecret', `${prefix}/mysql`, {
      username: 'sunridge_app',
    }, 'password', 'MySQL application user credentials for Sunridge services.');
    this.jwtSecret = this.generatedSecret('JwtSecret', `${prefix}/jwt`, {}, 'value', 'JWT signing key for Sunridge Commerce.');
    this.internalApiKeySecret = this.generatedSecret(
      'InternalApiKeySecret',
      `${prefix}/internal-api-key`,
      {},
      'value',
      'Shared internal API key for Sunridge service-to-service calls.',
    );

    this.externalConfigSecret = new secretsmanager.Secret(this, 'ExternalConfigSecret', {
      secretName: `${prefix}/external-config`,
      description: 'Provider credentials and public runtime settings for Sunridge. Populate these values in Secrets Manager.',
      secretStringValue: cdk.SecretValue.unsafePlainText(JSON.stringify({
        OPENAI_API_KEY: '',
        GITHUB_TOKEN: '',
        STRIPE_SECRET_KEY: '',
        STRIPE_WEBHOOK_SECRET: '',
        STRIPE_PUBLISHABLE_KEY: '',
        MAIL_HOST: '',
        MAIL_PORT: '587',
        NOTIFICATION_FROM: '',
        APP_FRONTEND_URL: '',
      })),
    });
    this.externalConfigSecret.applyRemovalPolicy(RemovalPolicy.RETAIN);

    new CfnOutput(this, 'MysqlApplicationSecretName', { value: this.mysqlApplicationSecret.secretName });
    new CfnOutput(this, 'JwtSecretName', { value: this.jwtSecret.secretName });
    new CfnOutput(this, 'InternalApiKeySecretName', { value: this.internalApiKeySecret.secretName });
    new CfnOutput(this, 'ExternalConfigSecretName', { value: this.externalConfigSecret.secretName });

    cdk.Tags.of(this).add('Application', 'sunridge-commerce');
    cdk.Tags.of(this).add('Environment', props.environmentName);
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }

  private generatedSecret(
    id: string,
    secretName: string,
    template: Record<string, string>,
    generatedKey: string,
    description: string,
  ): secretsmanager.Secret {
    const secret = new secretsmanager.Secret(this, id, {
      secretName,
      description,
      generateSecretString: {
        secretStringTemplate: JSON.stringify(template),
        generateStringKey: generatedKey,
        passwordLength: 48,
        excludeCharacters: " %+~`#$&*()|[]{}:;<>?!'/@\\\"",
      },
    });
    secret.applyRemovalPolicy(RemovalPolicy.RETAIN);
    return secret;
  }
}
