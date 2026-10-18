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
        log.info("Publicando eventForDiagnostics para serviceOrderId={} na fila de produção: {}",
                serviceOrderId, productionQueue);
        publishEvent(
                productionQueue,
                EVENT_FOR_DIAGNOSTICS,
                EventForDiagnosticsMessage.builder().serviceOrderId(serviceOrderId).build()
        );
    }

    public void publishEvent(String targetQueue, String eventType, Object payload) {
        log.info("Publicando evento {} na fila {}", eventType, targetQueue);

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

            log.info("Evento {} publicado com sucesso na queueUrl={}", eventType, queueUrl);

        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar payload da mensagem para eventType={}", eventType, e);
            throw new RuntimeException("Erro ao serializar mensagem do tipo " + eventType, e);
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} na fila SQS {}", eventType, targetQueue, e);
            throw new RuntimeException("Erro ao publicar " + eventType + " no SQS", e);
        }
    }

    private String resolveQueueUrl(String queueNameOrUrl) {
        if (queueNameOrUrl == null || queueNameOrUrl.isBlank()) {
            throw new IllegalArgumentException("Nome ou URL da fila não deve ser vazio");
        }

        if (queueNameOrUrl.startsWith("http://") || queueNameOrUrl.startsWith("https://")) {
            return queueNameOrUrl;
        }

        return resolvedQueueUrls.computeIfAbsent(queueNameOrUrl, name -> {
            log.debug("Resolvendo URL da fila SQS para o nome: {}", name);
            return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()).queueUrl();
        });
    }
}
