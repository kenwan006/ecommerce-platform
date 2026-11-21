#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { SunridgeApplicationSecretsStack } from '../lib/sunridge-application-secrets-stack';
import { SunridgeComputeStack } from '../lib/sunridge-compute-stack';
import { SunridgeDatabaseStack } from '../lib/sunridge-database-stack';
import { SunridgeRegistryStack } from '../lib/sunridge-registry-stack';

const app = new cdk.App();
const environmentName = app.node.tryGetContext('environmentName') ?? 'dev';
const deletionProtection = app.node.tryGetContext('deletionProtection') === true;

const databaseStack = new SunridgeDatabaseStack(app, `SunridgeDatabase-${environmentName}`, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
  description: 'Network, database, and database-secret foundation for Sunridge Commerce.',
  environmentName,
  deletionProtection,
});

const applicationSecretsStack = new SunridgeApplicationSecretsStack(app, `SunridgeApplicationSecrets-${environmentName}`, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
  description: 'Runtime secrets for Sunridge Commerce services.',
  environmentName,
});

new SunridgeRegistryStack(app, `SunridgeRegistry-${environmentName}`, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
  description: 'Private ECR repositories for Sunridge Commerce service images.',
  environmentName,
});

new SunridgeComputeStack(app, `SunridgeCompute-${environmentName}`, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
  description: 'Initial SSM-managed EC2 host for Sunridge Commerce.',
  environmentName,
  databaseStack,
  applicationSecretsStack,
});
