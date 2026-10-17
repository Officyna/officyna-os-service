package br.com.officyna.infrastructure.client.production;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductionLaborClientGatewayTest {

    private MockRestServiceServer mockServer;
    private ProductionLaborClientGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.gateway = new ProductionLaborClientGateway(builder, "http://localhost:8082");
    }

    @Test
    @DisplayName("Deve retornar snapshot do serviço com sucesso quando a resposta for 200")
    void findById_WhenSuccess_ShouldReturnLaborDetailDTO() {
        String laborId = "lab-100";
        String jsonResponse = """
                {
                    "id": "lab-100",
                    "name": "Troca de Pastilha",
                    "description": "Substituicao de pastilhas dianteiras",
                    "price": 250.00,
                    "executionTimeInDays": 1,
                    "active": true
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/labors/" + laborId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<LaborDetailDTO> result = gateway.findById(laborId);

        assertThat(result).isPresent();
        LaborDetailDTO dto = result.get();
        assertThat(dto.getLaborId()).isEqualTo("lab-100");
        assertThat(dto.getName()).isEqualTo("Troca de Pastilha");
        assertThat(dto.getDescription()).isEqualTo("Substituicao de pastilhas dianteiras");
        assertThat(dto.getLaborPrice()).isEqualByComparingTo("250.00");

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando o serviço responder 404")
    void findById_WhenNotFound_ShouldReturnEmpty() {
        String laborId = "not-found";

        mockServer.expect(requestTo("http://localhost:8082/api/labors/" + laborId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Optional<LaborDetailDTO> result = gateway.findById(laborId);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve lançar exceção de negócio se o serviço retornado estiver inativo")
    void findById_WhenLaborIsInactive_ShouldThrowServiceOrderBusinessException() {
        String laborId = "inactive-1";
        String jsonResponse = """
                {
                    "id": "inactive-1",
                    "name": "Serviço Descontinuado",
                    "description": "Antigo",
                    "price": 50.00,
                    "active": false
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/labors/" + laborId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.findById(laborId))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("inativo na produção");

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve lançar ServiceOrderBusinessException em caso de erro 500 do servidor")
    void findById_WhenServerError_ShouldThrowServiceOrderBusinessException() {
        String laborId = "error-id";

        mockServer.expect(requestTo("http://localhost:8082/api/labors/" + laborId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> gateway.findById(laborId))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("Falha na comunicação com o serviço de produção");

        mockServer.verify();
    }
}
