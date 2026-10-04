package br.com.officyna.domain.serviceorder.mapper;

import br.com.officyna.api.serviceorder.resources.ExistServiceOrderRequest;
import br.com.officyna.api.serviceorder.resources.NewServiceOrderRequest;
import br.com.officyna.domain.serviceorder.dto.CustomerDTO;
import br.com.officyna.domain.serviceorder.dto.LaborsDTO;
import br.com.officyna.domain.serviceorder.dto.MechanicDTO;
import br.com.officyna.domain.serviceorder.dto.VehicleDTO;
import br.com.officyna.domain.serviceorder.entity.ServiceOrder;
import org.springframework.stereotype.Component;

@Component
public class ServiceOrderMapper {

    public ServiceOrder toCreateEntity(NewServiceOrderRequest request, VehicleDTO vehicle, CustomerDTO customer, LaborsDTO labors){
        return ServiceOrder.builder()
                .vehicle(vehicle)
                .customer(customer)
                .informationText(request.getInformationText())
                .labors(labors)
                .build();
    }

    public ServiceOrder toUpdateEntity(ExistServiceOrderRequest request, ServiceOrder entity, MechanicDTO mechanic){
        entity.setInformationText(request.getInformationText());
        entity.setMechanic(mechanic);
        return entity;
    }
}