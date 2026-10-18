package br.com.officyna.infrastructure.messaging.publisher;

import br.com.officyna.domain.serviceorder.gateway.ServiceOrderEventPublisherGateway;
import br.com.officyna.infrastructure.messaging.dto.EventForDiagnosticsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServiceOrderSqsPublisherGateway implements ServiceOrderEventPublisherGateway {

    public static final String EVENT_TYPE_HEADER = "eventType";
    public static final String EVENT_FOR_DIAGNOSTICS = "eventForDiagnostics";

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String productionQueue;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();

    public ServiceOrderSqsPublisherGateway(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.production-queue:production-service-queue}") String productionQueue) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.productionQueue = productionQueue;
    }

    @Override
    public void publishForDiagnostics(String serviceOrderId) {
        log.info("Publishing eventForDiagnostics for serviceOrderId={} to production queue: {}",
                serviceOrderId, productionQueue);
        publishEvent(
                productionQueue,
                EVENT_FOR_DIAGNOSTICS,
                EventForDiagnosticsMessage.builder().serviceOrderId(serviceOrderId).build()
        );
    }

    public void publishEvent(String targetQueue, String eventType, Object payload) {
        log.info("Publishing event {} to queue {}", eventType, targetQueue);

        try {
            String messageBody = objectMapper.writeValueAsString(payload);
            String queueUrl = resolveQueueUrl(targetQueue);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(Map.of(
                            EVENT_TYPE_HEADER, MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(eventType)
                                    .build()
                    ))
                    .build();

            sqsClient.sendMessage(request);

            log.info("Event {} published successfully to queueUrl={}", eventType, queueUrl);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message payload for eventType={}", eventType, e);
            throw new RuntimeException("Error serializing " + eventType + " message", e);
        } catch (Exception e) {
            log.error("Failed to publish event {} to SQS queue {}", eventType, targetQueue, e);
            throw new RuntimeException("Error publishing " + eventType + " to SQS", e);
        }
    }

    private String resolveQueueUrl(String queueNameOrUrl) {
        if (queueNameOrUrl == null || queueNameOrUrl.isBlank()) {
            throw new IllegalArgumentException("Queue name or URL must not be empty");
        }

        if (queueNameOrUrl.startsWith("http://") || queueNameOrUrl.startsWith("https://")) {
            return queueNameOrUrl;
        }

        return resolvedQueueUrls.computeIfAbsent(queueNameOrUrl, name -> {
            log.debug("Resolving SQS queue URL for queue name: {}", name);
            return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()).queueUrl();
        });
    }
}
