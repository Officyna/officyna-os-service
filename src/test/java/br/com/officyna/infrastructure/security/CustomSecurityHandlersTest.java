package br.com.officyna.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomSecurityHandlersTest {

    @InjectMocks
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @InjectMocks
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;

    @BeforeEach
    void setUp() throws Exception {
        stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        lenient().when(response.getWriter()).thenReturn(printWriter);
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURI()).thenReturn("/api/serviceorder");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("CustomAuthenticationEntryPoint deve retornar 401 com JSON de ErrorResponse")
    void commence_ShouldWrite401Json() throws Exception {
        MDC.put("correlationId", "corr-sec-401");
        when(request.getAttribute("auth_error_message")).thenReturn("Token expired");

        authenticationEntryPoint.commence(request, response, new BadCredentialsException("Bad token"));

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");

        String json = stringWriter.toString();
        assertThat(json).contains("\"status\":401");
        assertThat(json).contains("\"correlationId\":\"corr-sec-401\"");
        assertThat(json).contains("\"path\":\"/api/serviceorder\"");
        assertThat(json).contains("Token expired");
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler deve retornar 403 com JSON de ErrorResponse")
    void handle_ShouldWrite403Json() throws Exception {
        MDC.put("correlationId", "corr-sec-403");

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Forbidden"));

        verify(response).setStatus(403);
        verify(response).setContentType("application/json");

        String json = stringWriter.toString();
        assertThat(json).contains("\"status\":403");
        assertThat(json).contains("\"correlationId\":\"corr-sec-403\"");
        assertThat(json).contains("\"path\":\"/api/serviceorder\"");
        assertThat(json).contains("insufficient permissions");
    }
}
