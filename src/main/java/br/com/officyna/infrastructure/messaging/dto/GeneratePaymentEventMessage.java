package br.com.officyna.infrastructure.messaging.dto;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePaymentEventMessage {

    private String serviceOrderId;

    private List<LaborDetailDTO> labors;

    @JsonAlias({"sypplyes", "suppliesDetails", "supplies"})
    private List<SupplyDetailDTO> supplies;
}
