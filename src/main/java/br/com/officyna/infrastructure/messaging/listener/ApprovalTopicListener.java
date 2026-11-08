package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import br.com.officyna.domain.serviceorder.service.ServiceOrderService;
import br.com.officyna.infrastructure.messaging.dto.ApprovalEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "aws.sqs.listener.enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalTopicListener extends AbstractSqsQueueListener<ApprovalEventMessage> {

    private final ServiceOrderService serviceOrderService;

    public ApprovalTopicListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            @Value("${aws.sqs.queues.topic-aprovacao:topicAprovacao}") String queueName,
            @Value("${aws.sqs.listener.wait-time-seconds:5}") int waitTimeSeconds) {
        super(sqsClient, objectMapper, queueName, waitTimeSeconds, ApprovalEventMessage.class);
        this.serviceOrderService = serviceOrderService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.listener.poll-delay-ms:2000}")
    public void poll() {
        pollMessages();
    }

    @Override
    protected void processPayload(ApprovalEventMessage message) {
        log.info("Processando mensagem da fila/tópico topicAprovacao para serviceOrderId={}, statusPayment={}",
                message.getServiceOrderId(), message.getStatusPayment());

        String statusPayment = message.getStatusPayment();
        if (statusPayment == null || "APROVED".equalsIgnoreCase(statusPayment) || "APROVADO".equalsIgnoreCase(statusPayment)) {
            serviceOrderService.updateStatus(message.getServiceOrderId(), ServiceOrderStatus.APROVADA);
            log.info("Status da ordem de serviço {} atualizado para APROVADA", message.getServiceOrderId());
        } else if ("RECUSED".equalsIgnoreCase(statusPayment) || "RECUSADO".equalsIgnoreCase(statusPayment)) {
            serviceOrderService.updateStatus(message.getServiceOrderId(), ServiceOrderStatus.RECUSADA);
            log.info("Status da ordem de serviço {} atualizado para RECUSADA", message.getServiceOrderId());
        } else {
            log.warn("statusPayment não reconhecido: {} para ordem de serviço: {}", statusPayment, message.getServiceOrderId());
        }
    }
}
