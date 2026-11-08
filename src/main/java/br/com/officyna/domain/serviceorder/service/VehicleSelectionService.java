package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.domain.vehicle.entity.Vehicle;
import br.com.officyna.domain.vehicle.service.VehicleService;
import br.com.officyna.domain.serviceorder.dto.VehicleDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VehicleSelectionService {
    private final VehicleService vehicleService;

    public VehicleSelectionService(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    public VehicleDTO getVehicle(String id) {

        log.info("Finding vehicle by id: {}", id);

        Vehicle vehicle = vehicleService.findById(id);

        log.debug("Vehicle found by id: {}", id);

        return new VehicleDTO(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor()
        );
    }

}
