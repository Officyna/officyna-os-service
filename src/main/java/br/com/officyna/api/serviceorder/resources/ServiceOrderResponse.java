package br.com.officyna.api.serviceorder.resources;

import br.com.officyna.domain.serviceorder.dto.*;


public record ServiceOrderResponse (

    String serviceOrderId,

    String serviceOrderNumber,

    CustomerDTO customer,

    MechanicDTO mechanic,

    VehicleDTO vehicle,

    LaborsDTO labors,

    SupplyDTO supplys,

    String informationText,

    String serviceOrderStatus,

    String statusDate,

    String totalBudgetAmount,

    String createdAt
){}
