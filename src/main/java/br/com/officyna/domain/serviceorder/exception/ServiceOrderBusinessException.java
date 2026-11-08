package br.com.officyna.domain.serviceorder.exception;

public class ServiceOrderBusinessException extends RuntimeException {

    public ServiceOrderBusinessException(String message) {
        super(message);
    }

    public ServiceOrderBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
