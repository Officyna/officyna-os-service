package br.com.officyna.infrastructure.messaging.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractSqsQueueListener<T> {

    protected final SqsClient sqsClient;
    protected final ObjectMapper objectMapper;
    protected final String queueName;
    protected final int waitTimeSeconds;
    protected final Class<T> payloadClass;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();

    protected AbstractSqsQueueListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            String queueName,
            int waitTimeSeconds,
            Class<T> payloadClass) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
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
        try {
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
        }
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
}
