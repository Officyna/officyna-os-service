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
public class BudgetGeneratedEventMessage {

    private String correlationId;

    private String serviceOrderId;

    private Totals totals;

    private TaxDetails taxDetails;

    @JsonAlias({"totalLaborsAmount", "total_labors_amount", "total_labor_amount"})
    private BigDecimal totalLaborAmount;

    @JsonAlias({"totalSuppliesAmount", "total_supply_amount"})
    private BigDecimal totalSupplyAmount;

    @JsonAlias({"total_taxes_amount"})
    private BigDecimal totalTaxesAmount;

    @JsonAlias({"total_budget_amount"})
    private BigDecimal totalBudgetAmount;

    @JsonAlias({"paymentLink", "payment_link", "paymentUrl"})
    private String paymentLink;

    @JsonAlias({"qrCode", "qr_code"})
    private String qrCode;

    @JsonAlias({"qrCodeBase64", "qr_code_base64"})
    private String qrCodeBase64;

    public BigDecimal getTotalLaborAmount() {
        if (totals != null && totals.getTotalLaborsAmount() != null) {
            return totals.getTotalLaborsAmount();
        }
        return totalLaborAmount;
    }

    public BigDecimal getTotalSupplyAmount() {
        if (totals != null && totals.getTotalSupplyAmount() != null) {
            return totals.getTotalSupplyAmount();
        }
        return totalSupplyAmount;
    }

    public BigDecimal getTotalTaxesAmount() {
        if (totals != null && totals.getTotalTaxesAmount() != null) {
            return totals.getTotalTaxesAmount();
        }
        return totalTaxesAmount;
    }

    public BigDecimal getTotalBudgetAmount() {
        if (totals != null && totals.getTotalBudgetAmount() != null) {
            return totals.getTotalBudgetAmount();
        }
        return totalBudgetAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Totals {

        @JsonAlias({"totalLaborAmount", "total_labors_amount", "total_labor_amount"})
        private BigDecimal totalLaborsAmount;

        @JsonAlias({"totalSuppliesAmount", "total_supply_amount"})
        private BigDecimal totalSupplyAmount;

        @JsonAlias({"total_taxes_amount"})
        private BigDecimal totalTaxesAmount;

        @JsonAlias({"total_budget_amount"})
        private BigDecimal totalBudgetAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaxDetails {

        private BigDecimal iss;

        private BigDecimal icms;

        private BigDecimal pisCofins;
    }
}
