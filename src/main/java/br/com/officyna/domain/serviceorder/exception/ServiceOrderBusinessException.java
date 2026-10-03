package br.com.officyna.domain.serviceorder.exception;

public class ServiceOrderBusinessException extends RuntimeException {

    public ServiceOrderBusinessException(String message) {
        super(message);
    }
}
