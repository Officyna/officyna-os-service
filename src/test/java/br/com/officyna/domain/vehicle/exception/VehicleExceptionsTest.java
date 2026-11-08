package br.com.officyna.domain.vehicle.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleExceptionsTest {

    @Test
    void businessException_ShouldExposeMessage() {
        VehicleBusinessException ex = new VehicleBusinessException("regra de negócio violada");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("regra de negócio violada");
    }

    @Test
    void notFoundException_ShouldBuildMessageFromId() {
        VehicleNotFoundException ex = VehicleNotFoundException.of("123");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Vehicle not found with id: 123");
    }

    @Test
    void businessException_ShouldSupportCause() {
        Throwable cause = new IllegalArgumentException("invalid plate");
        VehicleBusinessException ex = new VehicleBusinessException("placa invalida", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("placa invalida");
    }

    @Test
    void notFoundException_ShouldSupportCause() {
        Throwable cause = new RuntimeException("db error");
        VehicleNotFoundException ex = new VehicleNotFoundException("not found", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }
}
