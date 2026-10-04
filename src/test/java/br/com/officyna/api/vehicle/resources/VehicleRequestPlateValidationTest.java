package br.com.officyna.api.vehicle.resources;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleRequestPlateValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private VehicleRequest buildRequest(String plate) {
        return new VehicleRequest("customer-id", plate, "Toyota", "Corolla", 2020, "Prata");
    }

    private Set<ConstraintViolation<VehicleRequest>> plateViolations(String plate) {
        return validator.validate(buildRequest(plate))
                .stream()
                .filter(v -> v.getPropertyPath().toString().equals("plate"))
                .collect(Collectors.toSet());
    }

    // â”€â”€â”€ formatos vÃ¡lidos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @ParameterizedTest
    @DisplayName("Deve aceitar placa no formato antigo com hÃ­fen (ABC-1234)")
    @ValueSource(strings = {"ABC-1234", "XYZ-9999", "AAA-0000"})
    void shouldPass_OldFormatWithHyphen(String plate) {
        assertTrue(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve aceitar placa no formato antigo sem hÃ­fen (ABC1234)")
    @ValueSource(strings = {"ABC1234", "XYZ9999", "AAA0000"})
    void shouldPass_OldFormatWithoutHyphen(String plate) {
        assertTrue(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve aceitar placa no formato Mercosul (ABC1D23)")
    @ValueSource(strings = {"ABC1D23", "XYZ0A99", "AAA9Z00"})
    void shouldPass_MercosulFormat(String plate) {
        assertTrue(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve aceitar placa em letras minÃºsculas (normalizaÃ§Ã£o de entrada)")
    @ValueSource(strings = {"abc-1234", "abc1234", "abc1d23"})
    void shouldPass_LowercaseInput(String plate) {
        assertTrue(plateViolations(plate).isEmpty());
    }

    // â”€â”€â”€ formatos invÃ¡lidos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @ParameterizedTest
    @DisplayName("Deve rejeitar placa com menos de 3 letras iniciais")
    @ValueSource(strings = {"AB-1234", "A-1234", "AB1234"})
    void shouldFail_TooFewLetters(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve rejeitar placa com menos de 4 dÃ­gitos (formato antigo)")
    @ValueSource(strings = {"ABC-123", "ABC123", "ABC-12"})
    void shouldFail_TooFewDigitsOldFormat(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve rejeitar placa com mais de 4 dÃ­gitos seguidos")
    @ValueSource(strings = {"ABC-12345", "ABC12345"})
    void shouldFail_TooManyDigits(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve rejeitar placa com dÃ­gitos no inÃ­cio")
    @ValueSource(strings = {"1234ABC", "12AB345", "1BC-1234"})
    void shouldFail_DigitsAtStart(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve rejeitar placa vazia ou com apenas espaÃ§os")
    @ValueSource(strings = {" ", "  "})
    void shouldFail_BlankPlate(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Deve rejeitar formatos com caracteres especiais invÃ¡lidos")
    @ValueSource(strings = {"ABC_1234", "ABC.1234", "ABC/1234"})
    void shouldFail_InvalidSpecialChars(String plate) {
        assertFalse(plateViolations(plate).isEmpty());
    }
}