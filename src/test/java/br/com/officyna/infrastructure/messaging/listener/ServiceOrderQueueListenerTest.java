package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import br.com.officyna.domain.serviceorder.service.ServiceOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderQueueListenerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ServiceOrderService serviceOrderService;

    private ObjectMapper objectMapper;
    private ServiceOrderQueueListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new ServiceOrderQueueListener(
                sqsClient,
                objectMapper,
                serviceOrderService,
                "os-service-queue",
                5
        );
    }

    private Message createMessage(String id, String eventType, String body) {
        Message.Builder builder = Message.builder()
                .messageId(id)
                .receiptHandle("receipt-" + id)
                .body(body);

        if (eventType != null) {
            builder.messageAttributes(Map.of(
                    ServiceOrderQueueListener.EVENT_TYPE_HEADER,
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(eventType)
                            .build()
            ));
        }

        return builder.build();
    }

    @Test
    @DisplayName("Deve processar eventDiagnosis e deletar mensagem da fila")
    void poll_ShouldProcessEventDiagnosis() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-2", "eventDiagnosis", "{\"serviceOrderId\":\"so-200\",\"mechanicId\":\"mech-1\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).startDiagnosis("so-200", "mech-1");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve ignorar eventDiagnosisCompleted pois é lido exclusivamente pelo billing-service")
    void poll_ShouldIgnoreEventDiagnosisCompleted() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-3", "eventDiagnosisCompleted", "{\"serviceOrderId\":\"so-300\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verifyNoInteractions(serviceOrderService);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }


    @Test
    @DisplayName("Deve processar eventBugetGenerated e deletar mensagem da fila")
    void poll_ShouldProcessEventBugetGenerated() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        String json = """
                {
                  "serviceOrderId": "so-400",
                  "totalLaborAmount": 200.00,
                  "totalSupplyAmount": 100.00,
                  "totalBudgetAmount": 300.00
                }
                """;
        Message message = createMessage("msg-4", "eventBugetGenerated", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).applyBudget(
                eq("so-400"),
                eq(new BigDecimal("200.00")),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("300.00"))
        );
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve processar eventBudgetGenerated (grafia alternativa) e deletar mensagem da fila")
    void poll_ShouldProcessEventBudgetGenerated_AlternateSpelling() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        String json = """
                {
                  "serviceOrderId": "so-401",
                  "totalLaborAmount": 50.00,
                  "totalSupplyAmount": 25.00,
                  "totalBudgetAmount": 75.00
                }
                """;
        Message message = createMessage("msg-4b", "eventBudgetGenerated", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).applyBudget(
                eq("so-401"),
                eq(new BigDecimal("50.00")),
                eq(new BigDecimal("25.00")),
                eq(new BigDecimal("75.00"))
        );
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve processar PaymentConfirmedEvent e atualizar status para APROVADA")
    void poll_ShouldProcessPaymentConfirmedEvent() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-5", "PaymentConfirmedEvent", "{\"serviceOrderId\":\"so-500\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).updateStatus("so-500", ServiceOrderStatus.APROVADA);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Não deve chamar serviço nem deletar mensagem quando eventType não for fornecido")
    void poll_ShouldNotProcess_WhenEventTypeIsMissing() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-no-type", null, "{\"serviceOrderId\":\"so-1\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verifyNoInteractions(serviceOrderService);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Não deve lançar erro nem chamar serviço quando eventType for desconhecido")
    void poll_ShouldWarn_WhenEventTypeIsUnknown() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-unknown", "unknownEvent", "{\"serviceOrderId\":\"so-1\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verifyNoInteractions(serviceOrderService);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Não deve deletar mensagem quando corpo do JSON for inválido")
    void poll_ShouldNotDeleteMessage_WhenPayloadIsInvalid() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/os-service-queue").build());

        Message message = createMessage("msg-invalid", "eventDiagnosis", "not-a-valid-json");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verifyNoInteractions(serviceOrderService);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve capturar exceção de SQS e continuar sem erro")
    void poll_ShouldCatchException_WhenSqsFails() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenThrow(SqsException.builder().message("Network error").build());

        listener.poll();

        verifyNoInteractions(serviceOrderService);
    }
}
