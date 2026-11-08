package br.com.officyna.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalEventMessage {

    @JsonAlias({"serviceOrderOd", "service_order_id"})
    private String serviceOrderId;

    private PaymentInfo payment;

    @JsonAlias({"status_payment", "paymentStatus", "status"})
    private String statusPayment;

    @JsonAlias({"paymentDescription", "reason"})
    private String description;

    public ApprovalEventMessage(String serviceOrderId, String statusPayment) {
        this.serviceOrderId = serviceOrderId;
        this.statusPayment = statusPayment;
    }

    public String getStatusPayment() {
        if (payment != null && payment.getStatus() != null && !payment.getStatus().isBlank()) {
            return payment.getStatus();
        }
        return statusPayment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentInfo {

        private String paymentId;

        private String status;

        private String paymentMethod;

        private BigDecimal totalPaidAmount;

        private String paidAt;
    }
}
