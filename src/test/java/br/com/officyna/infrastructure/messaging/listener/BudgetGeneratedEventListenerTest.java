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
                                5);
        }

        private Message createMessage(String id, String body) {
                return Message.builder()
                                .messageId(id)
                                .receiptHandle("receipt-" + id)
                                .body(body)
                                .build();
        }

    @Test
    @DisplayName("Deve processar eventBugetGenerated com contrato aninhado totals e dados de pagamento")
    void poll_ShouldProcessBudgetGeneratedNestedContractAndAcknowledge() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/eventBugetGenerated").build());

        String json = """
                {
                  "serviceOrderId": "6af0b1a10be5150d141cafcd",
                  "totals": {
                    "totalLaborsAmount": 150,
                    "totalSupplyAmount": 119.34,
                    "totalTaxesAmount": 21.82,
                    "totalBudgetAmount": 291.16
                  },
                  "taxDetails": {
                    "iss": 7.5,
                    "icms": 14.32,
                    "pisCofins": 0
                  },
                  "paymentLink": "http://payment.com/pay/123",
                  "qrCode": "00020126580014BR.GOV.BCB.PIX",
                  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
                }
                """;
        Message message = createMessage("msg-1", json);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        listener.poll();

        verify(serviceOrderService).applyBudget(
                eq("6af0b1a10be5150d141cafcd"),
                eq(new BigDecimal("150")),
                eq(new BigDecimal("119.34")),
                eq(new BigDecimal("21.82")),
                eq(new BigDecimal("291.16")),
                eq("http://payment.com/pay/123"),
                eq("00020126580014BR.GOV.BCB.PIX"),
                eq("iVBORw0KGgoAAAANSUhEUgAA...")
        );
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

        @Test
        @DisplayName("Deve processar eventBugetGenerated com totalLaborsAmount (plural) sem paymentLink e publicar em DLQ em caso de erro")
        void poll_ShouldProcessTotalLaborsAmountAndPublishToDlqOnError() {
                BudgetGeneratedEventListener listenerWithDlq = new BudgetGeneratedEventListener(
                                sqsClient,
                                objectMapper,
                                serviceOrderService,
                                "eventBugetGenerated",
                                "eventBugetGeneratedDlq",
                                5);

                when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenAnswer(inv -> {
                        GetQueueUrlRequest req = inv.getArgument(0);
                        return GetQueueUrlResponse.builder().queueUrl("http://localhost:4566/" + req.queueName())
                                        .build();
                });

                String json = """
                                {
                                  "serviceOrderId": "so-401",
                                  "totalLaborsAmount": 220.00,
                                  "totalSupplyAmount": 180.00,
                                  "totalBudgetAmount": 400.00
                                }
                                """;
                Message message = createMessage("msg-2", json);
                when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());
                doThrow(new RuntimeException("Erro orçamento")).when(serviceOrderService).applyBudget(
                                eq("so-401"),
                                eq(new BigDecimal("220.00")),
                                eq(new BigDecimal("180.00")),
                                eq(null),
                                eq(new BigDecimal("400.00")),
                                eq(null),
                                eq(null),
                                eq(null));

                listenerWithDlq.poll();

                verify(sqsClient).sendMessage(argThat((SendMessageRequest req) -> req.queueUrl()
                                .equals("http://localhost:4566/eventBugetGeneratedDlq")
                                && req.messageBody().equals(json)));
                verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        }
}
