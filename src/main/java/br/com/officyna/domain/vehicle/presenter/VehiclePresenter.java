package br.com.officyna.domain.vehicle.presenter;

import br.com.officyna.api.vehicle.resources.VehicleResponse;
import br.com.officyna.domain.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehiclePresenter {

    public VehicleResponse toResponse(Vehicle entity) {
        return new VehicleResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getPlate(),
                entity.getBrand(),
                entity.getModel(),
                entity.getYear(),
                entity.getColor(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}