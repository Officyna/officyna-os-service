package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.domain.serviceorder.service.ServiceOrderService;
import br.com.officyna.infrastructure.messaging.dto.DiagnosisEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "aws.sqs.listener.enabled", havingValue = "true", matchIfMissing = true)
public class DiagnosisEventListener extends AbstractSqsQueueListener<DiagnosisEventMessage> {

    private final ServiceOrderService serviceOrderService;

    public DiagnosisEventListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            String queueName,
            int waitTimeSeconds) {
        super(sqsClient, objectMapper, queueName, waitTimeSeconds, DiagnosisEventMessage.class);
        this.serviceOrderService = serviceOrderService;
    }

    @Autowired
    public DiagnosisEventListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ServiceOrderService serviceOrderService,
            @Value("${aws.sqs.queues.event-diagnosis:eventDiagnosis}") String queueName,
            @Value("${aws.sqs.queues.event-diagnosis-dlq:eventDiagnosisDlq}") String dlqQueueName,
            @Value("${aws.sqs.listener.wait-time-seconds:5}") int waitTimeSeconds) {
        super(sqsClient, objectMapper, queueName, dlqQueueName, waitTimeSeconds, DiagnosisEventMessage.class);
        this.serviceOrderService = serviceOrderService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.listener.poll-delay-ms:2000}")
    public void poll() {
        pollMessages();
    }

    @Override
    protected void processPayload(DiagnosisEventMessage message) {
        log.info("Processando eventDiagnosis para serviceOrderId={}, mechanicId={}, mechanicName={}",
                message.getServiceOrderId(), message.getMechanicId(), message.getMechanicName());
        if (message.getMechanicName() != null && !message.getMechanicName().isBlank()) {
            serviceOrderService.startDiagnosis(
                    message.getServiceOrderId(),
                    message.getMechanicId(),
                    message.getMechanicName());
        } else {
            serviceOrderService.startDiagnosis(message.getServiceOrderId(), message.getMechanicId());
        }
    }
}
