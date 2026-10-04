package br.com.officyna.domain.serviceorder.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupplyDTO {

    private List<SupplyDetailDTO> supplysDetails;

    private BigDecimal totalSupplyAmount;
}
