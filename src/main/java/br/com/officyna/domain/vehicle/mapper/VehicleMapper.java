package br.com.officyna.domain.vehicle.mapper;


import br.com.officyna.api.vehicle.resources.VehicleRequest;
import br.com.officyna.domain.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public Vehicle toEntity(VehicleRequest request) {
        return Vehicle.builder()
                .customerId(request.customerId())
                .plate(request.plate().toUpperCase())
                .brand(request.brand())
                .model(request.model())
                .year(request.year())
                .color(request.color())
                .active(true)
                .build();
    }
}