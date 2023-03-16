#!/bin/sh
set -e
echo "building functions"
cd instrument-lambda && mvn clean package
echo "building CDK"
cd ../instrument-cdk && mvn clean package && cdk deploy --all --require-approval=never