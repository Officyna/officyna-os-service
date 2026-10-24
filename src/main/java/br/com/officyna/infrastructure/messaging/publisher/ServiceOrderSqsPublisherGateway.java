package br.com.officyna.infrastructure.messaging.publisher;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;
import br.com.officyna.domain.serviceorder.gateway.ServiceOrderEventPublisherGateway;
import br.com.officyna.infrastructure.messaging.dto.EventForDiagnosticsMessage;
import br.com.officyna.infrastructure.messaging.dto.GeneratePaymentEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import br.com.officyna.infrastructure.logging.CorrelationConstants;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServiceOrderSqsPublisherGateway implements ServiceOrderEventPublisherGateway {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String eventForDiagnosticsQueue;
    private final String eventGeneratePaymentQueue;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();

    public ServiceOrderSqsPublisherGateway(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.queues.event-for-diagnostics:eventForDiagnostics}") String eventForDiagnosticsQueue,
            @Value("${aws.sqs.queues.event-generate-payment:eventGeneratePayment}") String eventGeneratePaymentQueue) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.eventForDiagnosticsQueue = eventForDiagnosticsQueue;
        this.eventGeneratePaymentQueue = eventGeneratePaymentQueue;
    }

    @Override
    public void publishForDiagnostics(String serviceOrderId) {
        log.info("Publicando eventForDiagnostics para serviceOrderId={} na fila: {}",
                serviceOrderId, eventForDiagnosticsQueue);
        EventForDiagnosticsMessage payload = EventForDiagnosticsMessage.builder()
                .serviceOrderId(serviceOrderId)
                .status("RECEBIDA")
                .build();
        publishEvent(eventForDiagnosticsQueue, payload);
    }

    @Override
    public void publishGeneratePayment(String serviceOrderId, List<LaborDetailDTO> labors, List<SupplyDetailDTO> supplies) {
        log.info("Publicando eventGeneratePayment para serviceOrderId={} na fila: {}",
                serviceOrderId, eventGeneratePaymentQueue);
        GeneratePaymentEventMessage payload = GeneratePaymentEventMessage.builder()
                .serviceOrderId(serviceOrderId)
                .labors(labors)
                .supplies(supplies)
                .build();
        publishEvent(eventGeneratePaymentQueue, payload);
    }

    public void publishEvent(String targetQueue, Object payload) {
        log.info("Publicando mensagem na fila {}", targetQueue);

        try {
            String messageBody = objectMapper.writeValueAsString(payload);
            String queueUrl = resolveQueueUrl(targetQueue);

            String correlationId = MDC.get(CorrelationConstants.CORRELATION_ID_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            Map<String, MessageAttributeValue> messageAttributes = Map.of(
                    CorrelationConstants.CORRELATION_ID_HEADER, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(correlationId)
                            .build()
            );

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(messageAttributes)
                    .build();

            sqsClient.sendMessage(request);

            log.info("Mensagem publicada com sucesso na queueUrl={}, correlationId={}", queueUrl, correlationId);

        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar payload para a fila {}", targetQueue, e);
            throw new RuntimeException("Erro ao serializar mensagem para fila " + targetQueue, e);
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem na fila SQS {}", targetQueue, e);
            throw new RuntimeException("Erro ao publicar na fila SQS " + targetQueue, e);
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
