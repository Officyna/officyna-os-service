package br.com.officyna.infrastructure.messaging.listener;

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
public class RejectionTopicListener extends AbstractSqsQueueListener<ApprovalEventMessage> {

    private final ServiceOrderService serviceOrderService;

    public RejectionTopicListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            @Value("${aws.sqs.queues.topic-rejeicao:topicRejeicao}") String queueName,
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
        log.info("Processando mensagem da fila/tópico topicRejeicao para serviceOrderId={}, statusPayment={}, description={}",
                message.getServiceOrderId(), message.getStatusPayment(), message.getDescription());

        String statusPayment = message.getStatusPayment();
        if (statusPayment == null || "RECUSED".equalsIgnoreCase(statusPayment) || "RECUSADO".equalsIgnoreCase(statusPayment)) {
            serviceOrderService.handlePaymentRejection(
                    message.getServiceOrderId(),
                    statusPayment != null ? statusPayment : "RECUSED",
                    message.getDescription()
            );
            log.info("Status da ordem de serviço {} e pagamento atualizados para RECUSADA via topicRejeicao",
                    message.getServiceOrderId());
        } else {
            log.warn("statusPayment não reconhecido em topicRejeicao: {} para ordem de serviço: {}",
                    statusPayment, message.getServiceOrderId());
        }
    }
}

