#!/usr/bin/env bash
set -e

echo "Initializing SQS queues in LocalStack..."

awslocal sqs create-queue --queue-name os-service-queue
awslocal sqs create-queue --queue-name production-service-queue

echo "LocalStack SQS Queues created successfully:"
awslocal sqs list-queues
