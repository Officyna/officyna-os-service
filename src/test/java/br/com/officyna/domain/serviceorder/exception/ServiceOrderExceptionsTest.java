package br.com.officyna.domain.serviceorder.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceOrderExceptionsTest {

    @Test
    void businessException_ShouldExposeMessage() {
        ServiceOrderBusinessException ex = new ServiceOrderBusinessException("regra de negócio violada");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("regra de negócio violada");
    }

    @Test
    void notFoundException_ShouldBuildMessageFromId() {
        ServiceOrderNotFoundException ex = ServiceOrderNotFoundException.of("123");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Service Order not found with id: 123");
    }

    @Test
    void businessException_ShouldSupportCause() {
        Throwable cause = new IllegalArgumentException("invalid status");
        ServiceOrderBusinessException ex = new ServiceOrderBusinessException("regra violada", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("regra violada");
    }

    @Test
    void notFoundException_ShouldSupportCause() {
        Throwable cause = new RuntimeException("db error");
        ServiceOrderNotFoundException ex = new ServiceOrderNotFoundException("not found", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }
}
