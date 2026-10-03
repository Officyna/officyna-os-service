package br.com.officyna.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackExceptionHandlerTest {

    @InjectMocks
    private FallbackExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MDC.put("correlationId", "test-corr-123");
        lenient().when(request.getMethod()).thenReturn("POST");
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("handleValidation deve retornar 400 com mapa de erros e correlationId")
    void handleValidation_ShouldReturnBadRequest() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("obj", "name", "Name is required");
        FieldError error2 = new FieldError("obj", "name", "Min 3 characters");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().correlationId()).isEqualTo("test-corr-123");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().errors()).containsEntry("name", "Name is required; Min 3 characters");
    }

    @Test
    @DisplayName("handleConstraintViolation deve retornar 400 com propriedades mapeadas")
    void handleConstraintViolation_ShouldReturnBadRequest() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("document");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Invalid CPF format");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsEntry("document", "Invalid CPF format");
    }

    @Test
    @DisplayName("handleAuthenticationException deve retornar 401 com mensagem de credenciais inválidas")
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
        assertThat(response.getBody().correlationId()).isEqualTo("test-corr-123");
    }

    @Test
    @DisplayName("handleMessageNotReadable deve retornar 400 com mensagem legível")
    void handleMessageNotReadable_ShouldReturnBadRequest() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error");

        ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed JSON request or invalid field format");
    }

    @Test
    @DisplayName("handleTypeMismatch deve retornar 400 informando o tipo esperado")
    void handleTypeMismatch_ShouldReturnBadRequest() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("serviceOrderNumber");
        doReturn(Long.class).when(ex).getRequiredType();

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Parameter 'serviceOrderNumber' should be of type 'Long'");
    }

    @Test
    @DisplayName("handleMissingParameter deve retornar 400 informando o parâmetro ausente")
    void handleMissingParameter_ShouldReturnBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("status", "String");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Required query parameter 'status' is missing");
    }

    @Test
    @DisplayName("handleMethodNotSupported deve retornar 405 Method Not Allowed")
    void handleMethodNotSupported_ShouldReturnMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(405);
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported deve retornar 415 Unsupported Media Type")
    void handleMediaTypeNotSupported_ShouldReturnUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("text/xml");

        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(415);
    }

    @Test
    @DisplayName("handleDuplicateKey deve retornar 409 Conflict")
    void handleDuplicateKey_ShouldReturnConflict() {
        DuplicateKeyException ex = new DuplicateKeyException("E11000 duplicate key error");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateKey(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("A record with this unique identifier already exists");
    }

    @Test
    @DisplayName("handleIllegalStateAndArgument deve retornar 400 Bad Request")
    void handleIllegalStateAndArgument_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid status id: 99");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateAndArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid status id: 99");
    }

    @Test
    @DisplayName("handleGeneric deve retornar 500 com mensagem amigável e correlationId")
    void handleGeneric_ShouldReturnInternalServerError() {
        NullPointerException ex = new NullPointerException();

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Internal server error. Please try again later.");
        assertThat(response.getBody().correlationId()).isEqualTo("test-corr-123");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }
}
