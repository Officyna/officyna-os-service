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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalTopicListenerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ServiceOrderService serviceOrderService;

    private ObjectMapper objectMapper;
    private ApprovalTopicListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new ApprovalTopicListener(
                sqsClient,
                objectMapper,
                serviceOrderService,
                "topicAprovacao",
                5
        );
    }

    private Message createMessage(String id, String body) {
        return Message.builder()
                .messageId(id)
                .receiptHandle("receipt-" + id)
                .body(body)
                .build();
    }

    @Test
    @DisplayName("Deve processar mensagem direta de aprovação e atualizar status para APROVADA")
    void poll_ShouldProcessDirectApprovalMessage() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/topicAprovacao").build());

        String json = "{\"serviceOrderId\":\"so-500\",\"statusPayment\":\"APROVED\"}";
        Message message = createMessage("msg-1", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).updateStatus("so-500", ServiceOrderStatus.APROVADA);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve desempacotar envelope SNS e atualizar status para APROVADA")
    void poll_ShouldUnwrapSnsEnvelopeAndProcess() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/topicAprovacao").build());

        String innerMessage = "{\\\"serviceOrderOd\\\":\\\"so-501\\\",\\\"statusPayment\\\":\\\"APROVED\\\"}";
        String snsEnvelope = "{\"Type\":\"Notification\",\"MessageId\":\"sns-1\",\"Message\":\"" + innerMessage + "\"}";

        Message message = createMessage("msg-2", snsEnvelope);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).updateStatus("so-501", ServiceOrderStatus.APROVADA);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve atualizar status para RECUSADA quando statusPayment for RECUSED")
    void poll_ShouldUpdateToRecusada_WhenRecused() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/topicAprovacao").build());

        String json = "{\"serviceOrderId\":\"so-502\",\"statusPayment\":\"RECUSED\"}";
        Message message = createMessage("msg-3", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).updateStatus("so-502", ServiceOrderStatus.RECUSADA);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Não deve chamar updateStatus quando statusPayment for desconhecido")
    void poll_ShouldWarn_WhenStatusPaymentUnknown() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/topicAprovacao").build());

        String json = "{\"serviceOrderId\":\"so-503\",\"statusPayment\":\"UNKNOWN_STATUS\"}";
        Message message = createMessage("msg-4", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService, never()).updateStatus(anyString(), any());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
