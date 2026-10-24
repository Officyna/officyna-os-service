package br.com.officyna.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetGeneratedEventMessage {

    private String serviceOrderId;
    private BigDecimal totalLaborAmount;
    private BigDecimal totalSupplyAmount;
    private BigDecimal totalBudgetAmount;

    @JsonAlias({"paymentLink", "payment_link", "paymentUrl"})
    private String paymentLink;
}
