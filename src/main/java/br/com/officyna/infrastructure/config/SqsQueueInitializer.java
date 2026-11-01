package br.com.officyna.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "aws.sqs.init",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SqsQueueInitializer implements CommandLineRunner {

    private static final String DEFAULT_MAX_RECEIVE_COUNT = "3";

    private final SqsClient sqsClient;
    private final String eventForDiagnosticsQueue;
    private final String eventForDiagnosticsDlq;
    private final String eventGeneratePaymentQueue;
    private final String eventGeneratePaymentDlq;

    public SqsQueueInitializer(
            SqsClient sqsClient,
            @Value("${aws.sqs.queues.event-for-diagnostics:eventForDiagnostics}") String eventForDiagnosticsQueue,
            @Value("${aws.sqs.queues.event-for-diagnostics-dlq:eventForDiagnosticsDlq}") String eventForDiagnosticsDlq,
            @Value("${aws.sqs.queues.event-generate-payment:eventGeneratePayment}") String eventGeneratePaymentQueue,
            @Value("${aws.sqs.queues.event-generate-payment-dlq:eventGeneratePaymentDlq}") String eventGeneratePaymentDlq) {
        this.sqsClient = sqsClient;
        this.eventForDiagnosticsQueue = eventForDiagnosticsQueue;
        this.eventForDiagnosticsDlq = eventForDiagnosticsDlq;
        this.eventGeneratePaymentQueue = eventGeneratePaymentQueue;
        this.eventGeneratePaymentDlq = eventGeneratePaymentDlq;
    }

    @Override
    public void run(String... args) {
        log.info("Inicializando filas SQS e DLQs publicadas pelo os-service...");
        createQueueWithDlq(eventForDiagnosticsQueue, eventForDiagnosticsDlq);
        createQueueWithDlq(eventGeneratePaymentQueue, eventGeneratePaymentDlq);
    }

    protected void createQueueWithDlq(String rawQueueName, String rawDlqName) {
        String queueName = extractQueueName(rawQueueName);
        String dlqName = extractQueueName(rawDlqName);

        if (queueName.isBlank() || dlqName.isBlank()) {
            log.warn("Nome da fila principal ('{}') ou DLQ ('{}') está vazio. Ignorando criação.", rawQueueName, rawDlqName);
            return;
        }

        try {
            String dlqUrl = sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(dlqName)
                    .build()).queueUrl();

            String dlqArn = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(dlqUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build())
                    .attributes()
                    .get(QueueAttributeName.QUEUE_ARN);

            String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build()).queueUrl();

            String redrivePolicy = String.format(
                    "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"%s\"}",
                    dlqArn,
                    DEFAULT_MAX_RECEIVE_COUNT
            );

            sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributes(Map.of(QueueAttributeName.REDRIVE_POLICY, redrivePolicy))
                    .build());

            log.info("Fila SQS '{}' ({}) e DLQ '{}' ({}) garantidas com sucesso.",
                    queueName, queueUrl, dlqName, dlqUrl);
        } catch (Exception e) {
            log.error("Falha ao criar/configurar fila SQS '{}' e DLQ '{}': {}",
                    queueName, dlqName, e.getMessage(), e);
        }
    }

    private String extractQueueName(String queueNameOrUrl) {
        if (queueNameOrUrl == null || queueNameOrUrl.isBlank()) {
            return "";
        }
        String trimmed = queueNameOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            int lastSlash = trimmed.lastIndexOf('/');
            return lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        }
        return trimmed;
    }
}

