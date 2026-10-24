package br.com.officyna.domain.serviceorder.gateway;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.dto.SupplyDetailDTO;

import java.util.List;

public interface ServiceOrderEventPublisherGateway {

    void publishForDiagnostics(String serviceOrderId);

    void publishGeneratePayment(String serviceOrderId, List<LaborDetailDTO> labors, List<SupplyDetailDTO> supplies);
}
