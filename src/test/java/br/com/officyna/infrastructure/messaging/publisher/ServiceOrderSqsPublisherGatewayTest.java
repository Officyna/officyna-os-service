package br.com.officyna.infrastructure.messaging.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderSqsPublisherGatewayTest {

    @Mock
    private SqsClient sqsClient;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deve publicar eventForDiagnostics para production-service-queue com header eventType")
    void publishForDiagnostics_ShouldPublishToProductionQueueWithHeader() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/production-service-queue").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, objectMapper, "production-service-queue"
        );

        gateway.publishForDiagnostics("so-123");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo("http://localhost:4566/000000000000/production-service-queue");
        assertThat(request.messageBody()).contains("\"serviceOrderId\":\"so-123\"");
        assertThat(request.messageAttributes()).containsKey("eventType");
        assertThat(request.messageAttributes().get("eventType").stringValue()).isEqualTo("eventForDiagnostics");
    }

    @Test
    @DisplayName("Deve reutilizar cache de URL da fila na segunda chamada")
    void publishForDiagnostics_ShouldUseCachedQueueUrl() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/production-service-queue").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, objectMapper, "production-service-queue"
        );

        gateway.publishForDiagnostics("so-1");
        gateway.publishForDiagnostics("so-2");

        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("Deve usar URL direta quando configurada como HTTP")
    void publishForDiagnostics_ShouldUseDirectUrl_WhenConfiguredAsHttp() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, objectMapper, "http://sqs.us-east-1.amazonaws.com/123/direct-queue"
        );

        gateway.publishForDiagnostics("so-direct");

        verify(sqsClient, never()).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando envio SQS falhar")
    void publishForDiagnostics_ShouldThrowException_WhenSqsFails() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/production-service-queue").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("SQS error").build());

        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, objectMapper, "production-service-queue"
        );

        assertThatThrownBy(() -> gateway.publishForDiagnostics("so-fail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error publishing eventForDiagnostics to SQS");
    }

    @Test
    @DisplayName("Deve lançar exceção quando serialização JSON falhar")
    void publishForDiagnostics_ShouldThrowException_WhenJsonSerializationFails() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("JSON error") {});

        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, mockMapper, "production-service-queue"
        );

        assertThatThrownBy(() -> gateway.publishForDiagnostics("so-json-fail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error serializing eventForDiagnostics message");
    }

    @Test
    @DisplayName("Deve lançar exceção quando fila for vazia ou nula")
    void publishForDiagnostics_ShouldThrowException_WhenQueueIsNullOrEmpty() {
        ServiceOrderSqsPublisherGateway gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient, objectMapper, ""
        );

        assertThatThrownBy(() -> gateway.publishForDiagnostics("so-123"))
                .isInstanceOf(RuntimeException.class);
    }
}
