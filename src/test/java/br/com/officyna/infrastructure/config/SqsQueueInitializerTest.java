package br.com.officyna.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsQueueInitializerTest {

    @Mock
    private SqsClient sqsClient;

    private SqsQueueInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new SqsQueueInitializer(
                sqsClient,
                "eventForDiagnostics",
                "eventForDiagnosticsDlq",
                "eventGeneratePayment",
                "eventGeneratePaymentDlq"
        );
    }

    @Test
    @DisplayName("Deve criar as filas publicadas pelo os-service e suas respectivas DLQs com RedrivePolicy")
    void run_ShouldCreatePublishedQueuesAndDlqsWithRedrivePolicy() {
        when(sqsClient.createQueue(any(CreateQueueRequest.class))).thenAnswer(inv -> {
            CreateQueueRequest req = inv.getArgument(0);
            return CreateQueueResponse.builder()
                    .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/" + req.queueName())
                    .build();
        });

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenAnswer(inv -> {
            GetQueueAttributesRequest req = inv.getArgument(0);
            String queueName = req.queueUrl().substring(req.queueUrl().lastIndexOf('/') + 1);
            return GetQueueAttributesResponse.builder()
                    .attributes(Map.of(QueueAttributeName.QUEUE_ARN, "arn:aws:sqs:us-east-1:123456789012:" + queueName))
                    .build();
        });

        when(sqsClient.setQueueAttributes(any(SetQueueAttributesRequest.class)))
                .thenReturn(SetQueueAttributesResponse.builder().build());

        initializer.run();

        ArgumentCaptor<CreateQueueRequest> createCaptor = ArgumentCaptor.forClass(CreateQueueRequest.class);
        verify(sqsClient, times(4)).createQueue(createCaptor.capture());

        List<String> createdQueues = createCaptor.getAllValues().stream()
                .map(CreateQueueRequest::queueName)
                .toList();
        assertThat(createdQueues).containsExactly(
                "eventForDiagnosticsDlq",
                "eventForDiagnostics",
                "eventGeneratePaymentDlq",
                "eventGeneratePayment"
        );

        ArgumentCaptor<SetQueueAttributesRequest> setAttrCaptor = ArgumentCaptor.forClass(SetQueueAttributesRequest.class);
        verify(sqsClient, times(2)).setQueueAttributes(setAttrCaptor.capture());

        List<SetQueueAttributesRequest> attrRequests = setAttrCaptor.getAllValues();
        assertThat(attrRequests.get(0).queueUrl())
                .isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/eventForDiagnostics");
        assertThat(attrRequests.get(0).attributes().get(QueueAttributeName.REDRIVE_POLICY))
                .isEqualTo("{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:123456789012:eventForDiagnosticsDlq\",\"maxReceiveCount\":\"3\"}");

        assertThat(attrRequests.get(1).queueUrl())
                .isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/eventGeneratePayment");
        assertThat(attrRequests.get(1).attributes().get(QueueAttributeName.REDRIVE_POLICY))
                .isEqualTo("{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:123456789012:eventGeneratePaymentDlq\",\"maxReceiveCount\":\"3\"}");
    }

    @Test
    @DisplayName("Deve extrair o nome da fila quando configurado como URL completa")
    void createQueueWithDlq_ShouldExtractQueueNameFromUrl() {
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CreateQueueResponse.builder().queueUrl("http://localhost:4566/000000000000/q").build());
        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
                .thenReturn(GetQueueAttributesResponse.builder()
                        .attributes(Map.of(QueueAttributeName.QUEUE_ARN, "arn:aws:sqs:us-east-1:000000000000:eventForDiagnosticsDlq"))
                        .build());

        initializer.createQueueWithDlq(
                "https://sqs.us-east-1.amazonaws.com/123456789012/eventForDiagnostics",
                "http://localhost:4566/000000000000/eventForDiagnosticsDlq"
        );

        ArgumentCaptor<CreateQueueRequest> createCaptor = ArgumentCaptor.forClass(CreateQueueRequest.class);
        verify(sqsClient, times(2)).createQueue(createCaptor.capture());
        assertThat(createCaptor.getAllValues())
                .extracting(CreateQueueRequest::queueName)
                .containsExactly("eventForDiagnosticsDlq", "eventForDiagnostics");
    }

    @Test
    @DisplayName("Deve ignorar criação quando nome da fila ou DLQ for nulo ou vazio")
    void createQueueWithDlq_ShouldSkipWhenQueueOrDlqIsBlank() {
        initializer.createQueueWithDlq("", "eventForDiagnosticsDlq");
        initializer.createQueueWithDlq("eventForDiagnostics", "   ");
        initializer.createQueueWithDlq(null, "eventForDiagnosticsDlq");

        verifyNoInteractions(sqsClient);
    }

    @Test
    @DisplayName("Não deve propagar exceção quando SQS falhar na criação da fila")
    void createQueueWithDlq_ShouldNotThrowWhenSqsFails() {
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenThrow(SqsException.builder().message("Access Denied").build());

        assertThatCode(() -> initializer.run()).doesNotThrowAnyException();
    }
}

