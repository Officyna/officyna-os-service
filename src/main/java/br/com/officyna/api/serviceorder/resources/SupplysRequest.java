package br.com.officyna.api.serviceorder.resources;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplysRequest {

    private String id;

    private String name;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    public SupplysRequest(String id, Integer quantity) {
        this.id = id;
        this.quantity = quantity;
    }
}
