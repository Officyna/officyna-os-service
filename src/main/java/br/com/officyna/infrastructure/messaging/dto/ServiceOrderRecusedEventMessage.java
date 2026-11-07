package br.com.officyna.infrastructure.messaging.dto;

import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderRecusedEventMessage {

    private String serviceOrderId;

    @Builder.Default
    private String status = "RECUSADA";

    @JsonAlias({"sypplyes", "suppliesDetails", "supplies"})
    private List<RecusedSupplyItem> supplies;

    @JsonProperty("sypplyes")
    public List<RecusedSupplyItem> getSypplyes() {
        return supplies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecusedSupplyItem {
        private String id;
        private String supplyId;
        private String supplieId;
        private Integer quantity;
        private String name;
        private BigDecimal unitPrice;

        public static RecusedSupplyItem from(SupplyDetailDTO dto) {
            return RecusedSupplyItem.builder()
                    .id(dto.getId())
                    .supplyId(dto.getId())
                    .supplieId(dto.getId())
                    .quantity(dto.getQuantity())
                    .name(dto.getName())
                    .unitPrice(dto.getUnitPrice())
                    .build();
        }
    }
}

