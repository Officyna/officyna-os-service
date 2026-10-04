package br.com.officyna.domain.vehicle.exception;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String message) {
        super(message);
    }

    public static VehicleNotFoundException of(Object id) {
        return new VehicleNotFoundException("Vehicle not found with id: " + id);
    }
}
