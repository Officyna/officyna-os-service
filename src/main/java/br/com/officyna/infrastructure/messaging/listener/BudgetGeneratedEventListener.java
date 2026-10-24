package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.domain.serviceorder.service.ServiceOrderService;
import br.com.officyna.infrastructure.messaging.dto.BudgetGeneratedEventMessage;
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
public class BudgetGeneratedEventListener extends AbstractSqsQueueListener<BudgetGeneratedEventMessage> {

    private final ServiceOrderService serviceOrderService;

    public BudgetGeneratedEventListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            @Value("${aws.sqs.queues.event-buget-generated:eventBugetGenerated}") String queueName,
            @Value("${aws.sqs.listener.wait-time-seconds:5}") int waitTimeSeconds) {
        super(sqsClient, objectMapper, queueName, waitTimeSeconds, BudgetGeneratedEventMessage.class);
        this.serviceOrderService = serviceOrderService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.listener.poll-delay-ms:2000}")
    public void poll() {
        pollMessages();
    }

    @Override
    protected void processPayload(BudgetGeneratedEventMessage message) {
        log.info("Processando eventBugetGenerated para serviceOrderId={}, totalBudgetAmount={}",
                message.getServiceOrderId(), message.getTotalBudgetAmount());
        serviceOrderService.applyBudget(
                message.getServiceOrderId(),
                message.getTotalLaborAmount(),
                message.getTotalSupplyAmount(),
                message.getTotalBudgetAmount()
        );
    }
}
