#!/usr/bin/env bash
set -e

echo "Initializing SQS queues in LocalStack..."

awslocal sqs create-queue --queue-name eventForDiagnostics
awslocal sqs create-queue --queue-name eventDiagnosis
awslocal sqs create-queue --queue-name eventDiagnosisCompleted
awslocal sqs create-queue --queue-name eventGeneratePayment
awslocal sqs create-queue --queue-name eventBugetGenerated
awslocal sqs create-queue --queue-name topicAprovacao

echo "LocalStack SQS Queues created successfully:"
awslocal sqs list-queues
