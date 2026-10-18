package br.com.officyna.infrastructure.client.production;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderBusinessException;
import br.com.officyna.domain.serviceorder.gateway.ProductionLaborGateway;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
public class ProductionLaborClientGateway implements ProductionLaborGateway {

    private final RestClient restClient;

    @Autowired
    public ProductionLaborClientGateway(
            RestClient.Builder restClientBuilder,
            @Value("${services.production.url:http://localhost:8082}") String productionUrl) {
        this.restClient = restClientBuilder
                .baseUrl(productionUrl)
                .build();
    }

    public ProductionLaborClientGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<LaborDetailDTO> findById(String id) {
        log.info("Requesting labor snapshot from production-service for id: {}", id);

        try {
            LaborClientResponse response = restClient.get()
                    .uri("/api/labors/{id}", id)
                    .headers(this::propagateHeaders)
                    .retrieve()
                    .body(LaborClientResponse.class);

            if (response == null) {
                log.warn("Null response received from production-service for labor id: {}", id);
                return Optional.empty();
            }

            if (Boolean.FALSE.equals(response.active())) {
                log.warn("Labor id {} is marked as inactive in production-service", id);
                throw new ServiceOrderBusinessException("O serviço '" + response.name() + "' está inativo na produção.");
            }

            LaborDetailDTO detailDTO = LaborDetailDTO.builder()
                    .laborId(response.id())
                    .name(response.name())
                    .description(response.description())
                    .laborPrice(response.price() != null ? response.price() : BigDecimal.ZERO)
                    .situation(LaborSituation.PENDENTE)
                    .build();

            log.debug("Labor snapshot successfully converted for id: {}", id);

            return Optional.of(detailDTO);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Labor not found in production-service: id={}", id);
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.error("Client error calling production-service for labor id {}: status={}", id, e.getStatusCode());
            throw new ServiceOrderBusinessException("Erro ao consultar serviço de produção: " + e.getMessage());
        } catch (ServiceOrderBusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Communication failure with production-service for labor id {}: {}", id, e.getMessage());
            throw new ServiceOrderBusinessException("Falha na comunicação com o serviço de produção: " + e.getMessage());
        }
    }

    private void propagateHeaders(HttpHeaders headers) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    headers.set(HttpHeaders.AUTHORIZATION, auth);
                }
                String correlationId = request.getHeader("X-Correlation-ID");
                if (correlationId != null && !correlationId.isBlank()) {
                    headers.set("X-Correlation-ID", correlationId);
                }
            }
        } catch (Exception e) {
            log.debug("Could not propagate request headers: {}", e.getMessage());
        }
    }
}
