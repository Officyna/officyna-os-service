package br.com.officyna.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalEventMessage {

    @JsonAlias({"serviceOrderOd", "service_order_id"})
    private String serviceOrderId;

    @JsonAlias({"status_payment", "paymentStatus"})
    private String statusPayment;
}
