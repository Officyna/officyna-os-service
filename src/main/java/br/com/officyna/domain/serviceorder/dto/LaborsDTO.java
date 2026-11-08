package br.com.officyna.domain.serviceorder.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LaborsDTO {

    private List<LaborDetailDTO> laborsDetails;

    private BigDecimal totalLaborsAmount;

}
