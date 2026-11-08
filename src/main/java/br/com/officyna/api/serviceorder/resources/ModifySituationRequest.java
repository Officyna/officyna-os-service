package br.com.officyna.api.serviceorder.resources;

import br.com.officyna.domain.serviceorder.enums.LaborSituation;

public record ModifySituationRequest(
        String laborId,
        LaborSituation situation
) {}
