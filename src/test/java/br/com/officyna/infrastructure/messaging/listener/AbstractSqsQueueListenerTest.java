package br.com.officyna.infrastructure.messaging.listener;

import br.com.officyna.infrastructure.logging.CorrelationConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractSqsQueueListenerTest {

    @Mock
    private SqsClient sqsClient;

    private ObjectMapper objectMapper;
    private TestSqsQueueListener listener;
    private final AtomicReference<String> capturedMdcCorrelationId = new AtomicReference<>();

    static class TestPayload {
        private String message;

        public TestPayload() {}
        public TestPayload(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    class TestSqsQueueListener extends AbstractSqsQueueListener<TestPayload> {
        private boolean shouldThrow = false;

        public TestSqsQueueListener(SqsClient sqsClient, ObjectMapper objectMapper) {
            super(sqsClient, objectMapper, "test-queue", 5, TestPayload.class);
        }

        public void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        @Override
        protected void processPayload(TestPayload payload) {
            capturedMdcCorrelationId.set(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
            if (shouldThrow) {
                throw new RuntimeException("Erro proposital no processamento");
            }
        }
    }

    @BeforeEach
    void setUp() {
        MDC.clear();
        capturedMdcCorrelationId.set(null);
        objectMapper = new ObjectMapper();
        listener = new TestSqsQueueListener(sqsClient, objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve extrair correlationId do atributo da mensagem SQS e configurar no MDC durante o processamento")
    void pollMessages_ShouldExtractCorrelationIdFromMessageAttributes() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/test-queue").build());

        Message message = Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body("{\"message\":\"hello\"}")
                .messageAttributes(Map.of(
                        "correlationId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("corr-sqs-123")
                                .build()
                ))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.pollMessages();

        assertThat(capturedMdcCorrelationId.get()).isEqualTo("corr-sqs-123");
        assertThat(MDC.get(CorrelationConstants.CORRELATION_ID_KEY)).isNull();
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve gerar novo UUID para correlationId quando atributo estiver ausente na mensagem")
    void pollMessages_ShouldGenerateUuid_WhenAttributeAbsent() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/test-queue").build());

        Message message = Message.builder()
                .messageId("msg-2")
                .receiptHandle("receipt-2")
                .body("{\"message\":\"hello without correlation\"}")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.pollMessages();

        assertThat(capturedMdcCorrelationId.get()).isNotBlank();
        assertThat(MDC.get(CorrelationConstants.CORRELATION_ID_KEY)).isNull();
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve extrair correlationId de envelope SNS quando atributos estiverem no corpo JSON")
    void pollMessages_ShouldExtractCorrelationIdFromSnsEnvelope() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/test-queue").build());

        String snsBody = """
                {
                  "Type": "Notification",
                  "Message": "{\\"message\\":\\"from sns\\"}",
                  "MessageAttributes": {
                    "correlationId": {
                      "Type": "String",
                      "Value": "corr-sns-456"
                    }
                  }
                }
                """;

        Message message = Message.builder()
                .messageId("msg-3")
                .receiptHandle("receipt-3")
                .body(snsBody)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.pollMessages();

        assertThat(capturedMdcCorrelationId.get()).isEqualTo("corr-sns-456");
        assertThat(MDC.get(CorrelationConstants.CORRELATION_ID_KEY)).isNull();
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Deve limpar MDC mesmo se o processamento lançar exceção")
    void pollMessages_ShouldClearMdc_WhenProcessingFails() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/test-queue").build());

        Message message = Message.builder()
                .messageId("msg-4")
                .receiptHandle("receipt-4")
                .body("{\"message\":\"fail\"}")
                .messageAttributes(Map.of(
                        "correlationId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("corr-fail-999")
                                .build()
                ))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.setShouldThrow(true);
        listener.pollMessages();

        assertThat(capturedMdcCorrelationId.get()).isEqualTo("corr-fail-999");
        assertThat(MDC.get(CorrelationConstants.CORRELATION_ID_KEY)).isNull();
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
