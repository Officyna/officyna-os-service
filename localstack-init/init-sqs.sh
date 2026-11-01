#!/usr/bin/env bash
set -e

echo "Initializing SQS queues in LocalStack..."

create_queue_with_dlq() {
  local QUEUE="$1"
  local DLQ="${QUEUE}Dlq"

  local DLQ_URL
  DLQ_URL=$(awslocal sqs create-queue --queue-name "${DLQ}" --query "QueueUrl" --output text)
  local DLQ_ARN
  DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url "${DLQ_URL}" --attribute-names QueueArn --query "Attributes.QueueArn" --output text)

  local QUEUE_URL
  QUEUE_URL=$(awslocal sqs create-queue --queue-name "${QUEUE}" --query "QueueUrl" --output text)

  awslocal sqs set-queue-attributes \
    --queue-url "${QUEUE_URL}" \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
}

# Filas publicadas pelo os-service (com suas respectivas DLQs)
create_queue_with_dlq "eventForDiagnostics"
create_queue_with_dlq "eventGeneratePayment"

# Demais filas consumidas localmente
awslocal sqs create-queue --queue-name eventDiagnosis
awslocal sqs create-queue --queue-name eventDiagnosisCompleted
awslocal sqs create-queue --queue-name eventBugetGenerated
awslocal sqs create-queue --queue-name topicAprovacao

echo "LocalStack SQS Queues created successfully:"
awslocal sqs list-queues
