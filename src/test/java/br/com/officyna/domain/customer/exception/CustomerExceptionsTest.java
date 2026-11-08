package br.com.officyna.domain.customer.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerExceptionsTest {

    @Test
    void businessException_ShouldExposeMessage() {
        CustomerBusinessException ex = new CustomerBusinessException("regra de negócio violada");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("regra de negócio violada");
    }

    @Test
    void businessException_ShouldSupportCause() {
        Throwable cause = new IllegalArgumentException("invalid input");
        CustomerBusinessException ex = new CustomerBusinessException("regra violada", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("regra violada");
    }

    @Test
    void notFoundException_ShouldBuildMessageFromId() {
        CustomerNotFoundException ex = CustomerNotFoundException.of("123");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Customer not found with id: 123");
    }

    @Test
    void notFoundException_ShouldSupportCause() {
        Throwable cause = new RuntimeException("db error");
        CustomerNotFoundException ex = new CustomerNotFoundException("not found", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }
}
