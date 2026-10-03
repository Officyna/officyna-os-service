package br.com.officyna.domain.serviceorder.dto;

import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborDetailDTO {

    private String laborId;

    private String name;

    private String description;

    private BigDecimal laborPrice;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LaborSituation situation;

    private LocalDateTime situationDate;
}
