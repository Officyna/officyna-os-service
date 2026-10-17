package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderNotFoundException;
import br.com.officyna.domain.serviceorder.gateway.ProductionLaborGateway;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LaborSelectionService {

    private final ProductionLaborGateway productionLaborGateway;

    public LaborSelectionService(ProductionLaborGateway productionLaborGateway) {
        this.productionLaborGateway = productionLaborGateway;
    }

    public LaborDetailDTO getLaborSnapshot(String id) {
        log.info("Finding labor snapshot by id: {}", id);

        LaborDetailDTO snapshot = productionLaborGateway.findById(id)
                .orElseThrow(() -> {
                    log.warn("Labor not found in production service: {}", id);
                    return new ServiceOrderNotFoundException("Serviço não encontrado na produção: " + id);
                });

        log.debug("Labor snapshot retrieved successfully for id: {}", id);

        return snapshot;
    }
}
