#!/bin/sh
set -e
echo "building functions"
cd instrument-s3handler && mvn clean package
echo "building CDK"
cd ../instrument-s3handler-cdk && mvn clean package && cdk deploy