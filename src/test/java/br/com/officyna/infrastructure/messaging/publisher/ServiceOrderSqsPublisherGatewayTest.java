package br.com.officyna.infrastructure.messaging.publisher;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;
import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderSqsPublisherGatewayTest {

    @Mock
    private SqsClient sqsClient;

    private ObjectMapper objectMapper;
    private ServiceOrderSqsPublisherGateway gateway;

    @BeforeEach
    void setUp() {
        MDC.clear();
        objectMapper = new ObjectMapper();
        gateway = new ServiceOrderSqsPublisherGateway(
                sqsClient,
                objectMapper,
                "eventForDiagnostics",
                "eventGeneratePayment"
        );
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve publicar eventForDiagnostics para fila com header correlationId gerado")
    void publishForDiagnostics_ShouldPublishToQueueWithGeneratedCorrelationId() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/eventForDiagnostics").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        gateway.publishForDiagnostics("so-123");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo("http://localhost:4566/000000000000/eventForDiagnostics");
        assertThat(request.messageBody()).contains("\"serviceOrderId\":\"so-123\"");
        assertThat(request.messageBody()).contains("\"status\":\"RECEBIDA\"");
        assertThat(request.messageAttributes()).containsKey("correlationId");
        assertThat(request.messageAttributes().get("correlationId").stringValue()).isNotBlank();
    }

    @Test
    @DisplayName("Deve propagar correlationId do MDC na publicação do evento")
    void publishGeneratePayment_ShouldPropagateMdcCorrelationId() {
        MDC.put("correlationId", "corr-pub-789");
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/eventGeneratePayment").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-456").build());

        List<LaborDetailDTO> labors = List.of(
                LaborDetailDTO.builder().laborId("l-1").name("Troca de óleo").situation(LaborSituation.APROVADO).build()
        );
        List<SupplyDetailDTO> supplies = List.of(
                SupplyDetailDTO.builder().id("s-1").name("Filtro").quantity(1).unitPrice(new BigDecimal("50.00")).build()
        );

        gateway.publishGeneratePayment("so-456", labors, supplies);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo("http://localhost:4566/000000000000/eventGeneratePayment");
        assertThat(request.messageBody()).contains("\"serviceOrderId\":\"so-456\"");
        assertThat(request.messageBody()).contains("Troca de óleo");
        assertThat(request.messageBody()).contains("Filtro");
        assertThat(request.messageAttributes()).containsKey("correlationId");
        assertThat(request.messageAttributes().get("correlationId").stringValue()).isEqualTo("corr-pub-789");
    }

    @Test
    @DisplayName("Deve reutilizar cache de URL da fila na segunda chamada")
    void publishForDiagnostics_ShouldUseCachedQueueUrl() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/eventForDiagnostics").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

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

        ServiceOrderSqsPublisherGateway directGateway = new ServiceOrderSqsPublisherGateway(
                sqsClient,
                objectMapper,
                "http://sqs.us-east-1.amazonaws.com/123/eventForDiagnostics",
                "http://sqs.us-east-1.amazonaws.com/123/eventGeneratePayment"
        );

        directGateway.publishForDiagnostics("so-direct");

        verify(sqsClient, never()).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando envio SQS falhar")
    void publishForDiagnostics_ShouldThrowException_WhenSqsFails() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/000000000000/eventForDiagnostics").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("SQS error").build());

        assertThatThrownBy(() -> gateway.publishForDiagnostics("so-fail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao publicar na fila SQS eventForDiagnostics");
    }

    @Test
    @DisplayName("Deve lançar exceção quando serialização JSON falhar")
    void publishForDiagnostics_ShouldThrowException_WhenJsonSerializationFails() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("JSON error") {});

        ServiceOrderSqsPublisherGateway failGateway = new ServiceOrderSqsPublisherGateway(
                sqsClient,
                mockMapper,
                "eventForDiagnostics",
                "eventGeneratePayment"
        );

        assertThatThrownBy(() -> failGateway.publishForDiagnostics("so-json-fail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao serializar mensagem para fila eventForDiagnostics");
    }

    @Test
    @DisplayName("Deve lançar exceção quando fila for vazia ou nula")
    void publishForDiagnostics_ShouldThrowException_WhenQueueIsNullOrEmpty() {
        ServiceOrderSqsPublisherGateway emptyGateway = new ServiceOrderSqsPublisherGateway(
                sqsClient,
                objectMapper,
                "",
                ""
        );

        assertThatThrownBy(() -> emptyGateway.publishForDiagnostics("so-123"))
                .isInstanceOf(RuntimeException.class);
    }
}
