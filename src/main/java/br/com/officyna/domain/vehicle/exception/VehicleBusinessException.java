package br.com.officyna.domain.vehicle.exception;

public class VehicleBusinessException extends RuntimeException {

    public VehicleBusinessException(String message) {
        super(message);
    }

    public VehicleBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
