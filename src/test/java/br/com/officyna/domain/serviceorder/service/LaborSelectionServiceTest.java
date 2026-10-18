package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.domain.serviceorder.dto.LaborDetailDTO;
import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderNotFoundException;
import br.com.officyna.domain.serviceorder.gateway.ProductionLaborGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaborSelectionServiceTest {

    @Mock
    private ProductionLaborGateway productionLaborGateway;

    @InjectMocks
    private LaborSelectionService laborSelectionService;

    @Test
    @DisplayName("Deve retornar snapshot do serviço quando encontrado no production-service")
    void getLaborSnapshot_WhenFound_ShouldReturnLaborDetailDTO() {
        String laborId = "lab-1";
        LaborDetailDTO expected = LaborDetailDTO.builder()
                .laborId(laborId)
                .name("Alinhamento e Balanceamento")
                .description("Alinhamento 3D")
                .laborPrice(new BigDecimal("120.00"))
                .situation(LaborSituation.PENDENTE)
                .build();

        when(productionLaborGateway.findById(laborId)).thenReturn(Optional.of(expected));

        LaborDetailDTO result = laborSelectionService.getLaborSnapshot(laborId);

        assertThat(result).isNotNull();
        assertThat(result.getLaborId()).isEqualTo(laborId);
        assertThat(result.getName()).isEqualTo("Alinhamento e Balanceamento");
        assertThat(result.getLaborPrice()).isEqualByComparingTo("120.00");
        verify(productionLaborGateway).findById(laborId);
    }

    @Test
    @DisplayName("Deve lançar ServiceOrderNotFoundException quando o serviço não for encontrado na produção")
    void getLaborSnapshot_WhenNotFound_ShouldThrowServiceOrderNotFoundException() {
        String laborId = "not-found";

        when(productionLaborGateway.findById(laborId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> laborSelectionService.getLaborSnapshot(laborId))
                .isInstanceOf(ServiceOrderNotFoundException.class)
                .hasMessage("Serviço não encontrado na produção: " + laborId);

        verify(productionLaborGateway).findById(laborId);
    }
}
