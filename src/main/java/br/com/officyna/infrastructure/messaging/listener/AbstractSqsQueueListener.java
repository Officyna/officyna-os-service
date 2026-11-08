package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.infrastructure.logging.CorrelationConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.NewRelic;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractSqsQueueListener<T> {

    protected final SqsClient sqsClient;
    protected final ObjectMapper objectMapper;
    protected final String queueName;
    protected final String dlqQueueName;
    protected final int waitTimeSeconds;
    protected final Class<T> payloadClass;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();

    protected AbstractSqsQueueListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            String queueName,
            int waitTimeSeconds,
            Class<T> payloadClass) {
        this(sqsClient, objectMapper, queueName, null, waitTimeSeconds, payloadClass);
    }

    protected AbstractSqsQueueListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            String queueName,
            String dlqQueueName,
            int waitTimeSeconds,
            Class<T> payloadClass) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.dlqQueueName = dlqQueueName;
        this.waitTimeSeconds = waitTimeSeconds;
        this.payloadClass = payloadClass;
    }

    public void pollMessages() {
        try {
            String queueUrl = resolveQueueUrl(queueName);

            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(Math.max(0, Math.min(20, waitTimeSeconds)))
                    .messageAttributeNames("All")
                    .build());

            if (response.hasMessages() && !response.messages().isEmpty()) {
                log.info("Recebida(s) {} mensagem(ns) da fila SQS: {}", response.messages().size(), queueName);
                for (Message msg : response.messages()) {
                    processSingleMessage(queueUrl, msg);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao consultar fila SQS {}: {}", queueName, e.getMessage());
        }
    }

    protected void processSingleMessage(String queueUrl, Message msg) {
        String correlationId = extractCorrelationId(msg);
        String authorization = extractAuthorization(msg);
        try {
            MDC.put(CorrelationConstants.CORRELATION_ID_KEY, correlationId);
            if (authorization != null && !authorization.isBlank()) {
                MDC.put(CorrelationConstants.AUTHORIZATION_KEY, authorization);
            }
            NewRelic.addCustomParameter(CorrelationConstants.CORRELATION_ID_KEY, correlationId);

            log.info("Processando mensagem SQS id={} da fila: {}", msg.messageId(), queueName);
            String rawBody = msg.body();
            String payloadJson = extractPayload(rawBody);

            T payload = objectMapper.readValue(payloadJson, payloadClass);
            processPayload(payload);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .build());

            log.info("Mensagem SQS id={} processada e confirmada com sucesso da fila {}", msg.messageId(), queueName);
        } catch (Exception e) {
            log.error("Falha ao processar mensagem id={} da fila {}. Conteúdo: {}",
                    msg.messageId(), queueName, msg.body(), e);
            publishToDlq(msg, correlationId, authorization);
        } finally {
            MDC.remove(CorrelationConstants.CORRELATION_ID_KEY);
            MDC.remove(CorrelationConstants.AUTHORIZATION_KEY);
        }
    }

    protected void publishToDlq(Message msg, String correlationId, String authorization) {
        if (dlqQueueName == null || dlqQueueName.isBlank()) {
            return;
        }
        try {
            String dlqUrl = resolveQueueUrl(dlqQueueName);
            Map<String, MessageAttributeValue> attributes = new HashMap<>();
            if (correlationId != null && !correlationId.isBlank()) {
                attributes.put(
                        CorrelationConstants.CORRELATION_ID_HEADER,
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(correlationId)
                                .build());
            }
            if (authorization != null && !authorization.isBlank()) {
                attributes.put(
                        CorrelationConstants.AUTHORIZATION_HEADER,
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(authorization)
                                .build());
            }
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .messageBody(msg.body())
                    .messageAttributes(attributes)
                    .build());
            log.info("Mensagem id={} enviada para DLQ {} com correlationId={}",
                    msg.messageId(), dlqQueueName, correlationId);
        } catch (Exception dlqEx) {
            log.error("Falha ao publicar mensagem id={} na DLQ {}: {}",
                    msg.messageId(), dlqQueueName, dlqEx.getMessage(), dlqEx);
        }
    }

    protected String extractAuthorization(Message msg) {
        if (msg.hasMessageAttributes() && msg.messageAttributes() != null
                && msg.messageAttributes().containsKey(CorrelationConstants.AUTHORIZATION_HEADER)) {
            String val = msg.messageAttributes().get(CorrelationConstants.AUTHORIZATION_HEADER).stringValue();
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    protected String extractCorrelationId(Message msg) {
        if (msg.hasMessageAttributes() && msg.messageAttributes() != null) {
            if (msg.messageAttributes().containsKey(CorrelationConstants.CORRELATION_ID_HEADER)) {
                String val = msg.messageAttributes().get(CorrelationConstants.CORRELATION_ID_HEADER).stringValue();
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
            if (msg.messageAttributes().containsKey(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER)) {
                String val = msg.messageAttributes().get(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER)
                        .stringValue();
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }

        String rawBody = msg.body();
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(rawBody);
                if (root.has("MessageAttributes")) {
                    JsonNode msgAttrs = root.get("MessageAttributes");
                    if (msgAttrs.has(CorrelationConstants.CORRELATION_ID_HEADER)
                            && msgAttrs.get(CorrelationConstants.CORRELATION_ID_HEADER).has("Value")) {
                        String val = msgAttrs.get(CorrelationConstants.CORRELATION_ID_HEADER).get("Value").asText();
                        if (val != null && !val.isBlank()) {
                            return val;
                        }
                    }
                    if (msgAttrs.has(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER)
                            && msgAttrs.get(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER).has("Value")) {
                        String val = msgAttrs.get(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER).get("Value")
                                .asText();
                        if (val != null && !val.isBlank()) {
                            return val;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Continua para fallback UUID
            }
        }

        return UUID.randomUUID().toString();
    }

    private String extractPayload(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("Type") && "Notification".equals(root.get("Type").asText()) && root.has("Message")) {
                return root.get("Message").asText();
            }
        } catch (Exception ignored) {
            // Continua com o corpo original caso não seja um envelope SNS
        }
        return body;
    }

    protected abstract void processPayload(T payload);

    protected String resolveQueueUrl(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Nome ou URL da fila não deve ser vazio");
        }

        if (target.startsWith("http://") || target.startsWith("https://")) {
            return target;
        }

        return resolvedQueueUrls.computeIfAbsent(target, name -> {
            log.debug("Resolvendo URL da fila SQS para o nome: {}", name);
            return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()).queueUrl();
        });
    }

    public String getQueueName() {
        return queueName;
    }

    public String getDlqQueueName() {
        return dlqQueueName;
    }
}
