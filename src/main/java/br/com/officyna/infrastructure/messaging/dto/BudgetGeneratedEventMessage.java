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

    @JsonAlias({"totalLaborsAmount", "total_labors_amount", "total_labor_amount"})
    private BigDecimal totalLaborAmount;

    @JsonAlias({"totalSuppliesAmount", "total_supply_amount"})
    private BigDecimal totalSupplyAmount;

    @JsonAlias({"total_budget_amount"})
    private BigDecimal totalBudgetAmount;

    @JsonAlias({"paymentLink", "payment_link", "paymentUrl"})
    private String paymentLink;
}
