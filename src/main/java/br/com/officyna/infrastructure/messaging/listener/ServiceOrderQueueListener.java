package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import br.com.officyna.domain.serviceorder.service.ServiceOrderService;
import br.com.officyna.infrastructure.messaging.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "aws.sqs.listener.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceOrderQueueListener {

    public static final String EVENT_TYPE_HEADER = "eventType";

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ServiceOrderService serviceOrderService;
    private final String queueName;
    private final int waitTimeSeconds;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();

    public ServiceOrderQueueListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            @Value("${aws.sqs.inbound-queue:os-service-queue}") String queueName,
            @Value("${aws.sqs.listener.wait-time-seconds:5}") int waitTimeSeconds) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.serviceOrderService = serviceOrderService;
        this.queueName = queueName;
        this.waitTimeSeconds = waitTimeSeconds;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.listener.poll-delay-ms:2000}")
    public void poll() {
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

    private void processSingleMessage(String queueUrl, Message msg) {
        String eventType = null;
        try {
            if (msg.hasMessageAttributes() && msg.messageAttributes().containsKey(EVENT_TYPE_HEADER)) {
                eventType = msg.messageAttributes().get(EVENT_TYPE_HEADER).stringValue();
            }

            log.info("Processando mensagem SQS id={}, eventType={}", msg.messageId(), eventType);
            dispatch(eventType, msg.body());

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .build());

            log.info("Mensagem SQS id={} processada e confirmada com sucesso", msg.messageId());
        } catch (Exception e) {
            log.error("Falha ao processar mensagem id={} com eventType={} da fila {}. Conteúdo: {}",
                    msg.messageId(), eventType, queueName, msg.body(), e);
        }
    }

    public void dispatch(String eventType, String body) throws JsonProcessingException {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Atributo de mensagem 'eventType' está ausente ou vazio");
        }

        switch (eventType) {
            case "eventDiagnosis" -> {
                DiagnosisEventMessage message = objectMapper.readValue(body, DiagnosisEventMessage.class);
                serviceOrderService.startDiagnosis(message.getServiceOrderId(), message.getMechanicId());
            }
            case "eventDiagnosisCompleted" -> {
                log.info("Ignorando eventDiagnosisCompleted - este evento é processado exclusivamente pelo billing-service.");
            }
            case "eventBugetGenerated", "eventBudgetGenerated" -> {
                BudgetGeneratedEventMessage message = objectMapper.readValue(body, BudgetGeneratedEventMessage.class);
                serviceOrderService.applyBudget(
                        message.getServiceOrderId(),
                        message.getTotalLaborAmount(),
                        message.getTotalSupplyAmount(),
                        message.getTotalBudgetAmount()
                );
            }
            case "PaymentConfirmedEvent", "paymentConfirmed" -> {
                PaymentConfirmedEventMessage message = objectMapper.readValue(body, PaymentConfirmedEventMessage.class);
                serviceOrderService.updateStatus(message.getServiceOrderId(), ServiceOrderStatus.APROVADA);
            }
            default -> log.warn("eventType não reconhecido: {} na mensagem da fila {}", eventType, queueName);
        }
    }

    private String resolveQueueUrl(String target) {
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
}
