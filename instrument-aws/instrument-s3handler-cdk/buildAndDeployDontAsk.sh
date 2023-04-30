#!/bin/sh
set -e
cd .. 
cd .. 
mvn clean package
cd instrument-aws
cd instrument-s3-handler-cdk
mvn package
cdk deploy --all --require-approval=never