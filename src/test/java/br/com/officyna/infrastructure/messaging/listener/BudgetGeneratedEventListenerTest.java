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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetGeneratedEventListenerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ServiceOrderService serviceOrderService;

    private ObjectMapper objectMapper;
    private BudgetGeneratedEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new BudgetGeneratedEventListener(
                sqsClient,
                objectMapper,
                serviceOrderService,
                "eventBugetGenerated",
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
    @DisplayName("Deve processar eventBugetGenerated e aplicar orçamento")
    void poll_ShouldProcessBudgetGeneratedAndAcknowledge() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/eventBugetGenerated").build());

        String json = """
                {
                  "serviceOrderId": "so-400",
                  "totalLaborAmount": 220.00,
                  "totalSupplyAmount": 180.00,
                  "totalBudgetAmount": 400.00,
                  "paymentLink": "http://payment.com/pay/123"
                }
                """;
        Message message = createMessage("msg-1", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).applyBudget(
                eq("so-400"),
                eq(new BigDecimal("220.00")),
                eq(new BigDecimal("180.00")),
                eq(new BigDecimal("400.00"))
        );
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
