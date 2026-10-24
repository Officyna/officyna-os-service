package br.com.officyna.infrastructure.messaging.listener;

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
class DiagnosisEventListenerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ServiceOrderService serviceOrderService;

    private ObjectMapper objectMapper;
    private DiagnosisEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new DiagnosisEventListener(
                sqsClient,
                objectMapper,
                serviceOrderService,
                "eventDiagnosis",
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
    @DisplayName("Deve processar eventDiagnosis e deletar mensagem da fila")
    void poll_ShouldProcessEventDiagnosisAndAcknowledge() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/eventDiagnosis").build());

        String json = "{\"serviceOrderId\":\"so-200\",\"mechanicId\":\"mech-1\",\"mechanicName\":\"Joao\"}";
        Message message = createMessage("msg-1", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).startDiagnosis("so-200", "mech-1");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Não deve deletar mensagem quando corpo do JSON for inválido")
    void poll_ShouldNotDeleteMessage_WhenPayloadIsInvalid() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/eventDiagnosis").build());

        Message message = createMessage("msg-invalid", "invalid-json");
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
