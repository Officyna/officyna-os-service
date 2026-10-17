package br.com.officyna.domain.serviceorder.gateway;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;

import java.util.Optional;

public interface ProductionLaborGateway {

    Optional<LaborDetailDTO> findById(String id);
}
