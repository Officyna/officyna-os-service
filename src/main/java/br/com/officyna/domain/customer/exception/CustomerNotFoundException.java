package br.com.officyna.domain.customer.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CustomerNotFoundException of(Object id) {
        return new CustomerNotFoundException("Customer not found with id: " + id);
    }
}
