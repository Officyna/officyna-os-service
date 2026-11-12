package br.com.officyna.infrastructure.messaging.publisher;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;
import br.com.officyna.domain.serviceorder.gateway.ServiceOrderEventPublisherGateway;
import br.com.officyna.infrastructure.messaging.dto.EventForDiagnosticsMessage;
import br.com.officyna.infrastructure.messaging.dto.GeneratePaymentEventMessage;
import br.com.officyna.infrastructure.messaging.dto.ServiceOrderRecusedEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import br.com.officyna.infrastructure.logging.CorrelationConstants;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class ServiceOrderSqsPublisherGateway implements ServiceOrderEventPublisherGateway {

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String eventForDiagnosticsQueue;
    private final String eventGeneratePaymentQueue;
    private final String topicServiceOrderRecused;
    private final Map<String, String> resolvedQueueUrls = new ConcurrentHashMap<>();
    private volatile String cachedTopicServiceOrderRecusedArn;

    public ServiceOrderSqsPublisherGateway(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            String eventForDiagnosticsQueue,
            String eventGeneratePaymentQueue) {
        this(sqsClient, null, objectMapper, eventForDiagnosticsQueue, eventGeneratePaymentQueue, "topicServiceOrderRecused");
    }

    public ServiceOrderSqsPublisherGateway(
            SqsClient sqsClient,
            SnsClient snsClient,
            ObjectMapper objectMapper,
            String eventForDiagnosticsQueue,
            String eventGeneratePaymentQueue) {
        this(sqsClient, snsClient, objectMapper, eventForDiagnosticsQueue, eventGeneratePaymentQueue, "topicServiceOrderRecused");
    }

    @Autowired
    public ServiceOrderSqsPublisherGateway(
            SqsClient sqsClient,
            @Autowired(required = false) SnsClient snsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.queues.event-for-diagnostics:eventForDiagnostics}") String eventForDiagnosticsQueue,
            @Value("${aws.sqs.queues.event-generate-payment:eventGeneratePayment}") String eventGeneratePaymentQueue,
            @Value("${aws.sns.topics.topic-service-order-recused:topicServiceOrderRecused}") String topicServiceOrderRecused) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.eventForDiagnosticsQueue = eventForDiagnosticsQueue;
        this.eventGeneratePaymentQueue = eventGeneratePaymentQueue;
        this.topicServiceOrderRecused = topicServiceOrderRecused;
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
    public void publishGeneratePayment(String serviceOrderId, List<LaborDetailDTO> labors,
            List<SupplyDetailDTO> supplies) {
        publishGeneratePayment(serviceOrderId, null, null, labors, supplies);
    }

    @Override
    public void publishGeneratePayment(
            String serviceOrderId,
            String payerEmail,
            String customerDocument,
            List<LaborDetailDTO> labors,
            List<SupplyDetailDTO> supplies) {
        log.info("Publicando eventGeneratePayment para serviceOrderId={} na fila: {}",
                serviceOrderId, eventGeneratePaymentQueue);
        GeneratePaymentEventMessage payload = GeneratePaymentEventMessage.builder()
                .serviceOrderId(serviceOrderId)
                .payerEmail(payerEmail)
                .customerDocument(customerDocument)
                .labors(labors)
                .supplies(supplies)
                .build();
        publishEvent(eventGeneratePaymentQueue, payload);
    }

    @Override
    public void publishServiceOrderRecused(String serviceOrderId, List<SupplyDetailDTO> supplies) {
        log.info("Publicando compensação no tópico SNS {} para serviceOrderId={}",
                topicServiceOrderRecused, serviceOrderId);
        List<ServiceOrderRecusedEventMessage.RecusedSupplyItem> items = (supplies != null)
                ? supplies.stream()
                        .map(ServiceOrderRecusedEventMessage.RecusedSupplyItem::from)
                        .toList()
                : List.of();
        ServiceOrderRecusedEventMessage payload = ServiceOrderRecusedEventMessage.builder()
                .serviceOrderId(serviceOrderId)
                .status("RECUSADA")
                .supplies(items)
                .build();

        if (snsClient != null) {
            publishSnsEvent(resolveTopicArn(), payload);
        } else {
            publishEvent(topicServiceOrderRecused, payload);
        }
    }

    private void publishSnsEvent(String topicArn, Object payload) {
        try {
            String messageBody = objectMapper.writeValueAsString(payload);
            String correlationId = MDC.get(CorrelationConstants.CORRELATION_ID_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            Map<String, software.amazon.awssdk.services.sns.model.MessageAttributeValue> attributes = new HashMap<>();
            attributes.put(
                    CorrelationConstants.CORRELATION_ID_HEADER, software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(correlationId)
                            .build());

            String authorization = resolveAuthorizationHeader();
            if (authorization != null && !authorization.isBlank()) {
                attributes.put(
                        CorrelationConstants.AUTHORIZATION_HEADER, software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(authorization)
                                .build());
            }

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageBody)
                    .messageAttributes(attributes)
                    .build();

            snsClient.publish(request);
            log.info("Mensagem publicada com sucesso no tópico SNS topicArn={}, correlationId={}", topicArn, correlationId);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar mensagem para tópico SNS {}", topicArn, e);
            throw new RuntimeException("Erro ao serializar mensagem para tópico SNS " + topicArn, e);
        } catch (Exception e) {
            log.error("Erro ao publicar no tópico SNS {}: {}", topicArn, e.getMessage(), e);
            throw new RuntimeException("Erro ao publicar no tópico SNS " + topicArn, e);
        }
    }

    private String resolveTopicArn() {
        if (cachedTopicServiceOrderRecusedArn != null) {
            return cachedTopicServiceOrderRecusedArn;
        }
        if (topicServiceOrderRecused != null && topicServiceOrderRecused.startsWith("arn:aws:sns:")) {
            cachedTopicServiceOrderRecusedArn = topicServiceOrderRecused;
            return cachedTopicServiceOrderRecusedArn;
        }
        try {
            cachedTopicServiceOrderRecusedArn = snsClient.createTopic(
                    CreateTopicRequest.builder().name(topicServiceOrderRecused).build()
            ).topicArn();
            return cachedTopicServiceOrderRecusedArn;
        } catch (Exception e) {
            log.warn("Falha ao resolver ARN via createTopic para '{}': {}", topicServiceOrderRecused, e.getMessage());
            cachedTopicServiceOrderRecusedArn = topicServiceOrderRecused;
            return cachedTopicServiceOrderRecusedArn;
        }
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

            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put(
                    CorrelationConstants.CORRELATION_ID_HEADER, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(correlationId)
                            .build());

            String authorization = resolveAuthorizationHeader();
            if (authorization != null && !authorization.isBlank()) {
                messageAttributes.put(
                        CorrelationConstants.AUTHORIZATION_HEADER, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(authorization)
                                .build());
            }

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

    private String resolveAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    return auth;
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String credentials
                    && !credentials.isBlank()) {
                return credentials.startsWith("Bearer ") ? credentials : "Bearer " + credentials;
            }

            String mdcAuth = MDC.get(CorrelationConstants.AUTHORIZATION_KEY);
            if (mdcAuth != null && !mdcAuth.isBlank()) {
                return mdcAuth.startsWith("Bearer ") ? mdcAuth : "Bearer " + mdcAuth;
            }
        } catch (Exception e) {
            log.debug("Could not resolve Authorization header for SQS propagation: {}", e.getMessage());
        }
        return null;
    }
}
