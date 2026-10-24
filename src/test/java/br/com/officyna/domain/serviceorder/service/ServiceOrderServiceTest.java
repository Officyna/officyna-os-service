package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.domain.serviceorder.exception.ServiceOrderBusinessException;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderNotFoundException;
import br.com.officyna.api.serviceorder.resources.*;
import br.com.officyna.domain.serviceorder.dto.*;
import br.com.officyna.domain.serviceorder.entity.ServiceOrder;
import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import br.com.officyna.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.officyna.domain.serviceorder.repository.ServiceOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository repository;

    @Mock
    private ServiceOrderMapper mapper;

    @Mock
    private CustomerAndMecnichalService customerAndMecnichalService;

    @Mock
    private VehicleSelectionService vehicleSelectionService;

    @Mock
    private LaborSelectionService laborSelectionService;

    @Mock
    private br.com.officyna.domain.serviceorder.gateway.ServiceOrderEventPublisherGateway eventPublisherGateway;

    @InjectMocks
    private ServiceOrderService service;

    // ─────────────── helpers ───────────────

    private ServiceOrder buildEntity(String id, ServiceOrderStatus status) {
        ServiceOrder entity = new ServiceOrder();
        entity.setId(id);
        entity.setServiceOrderNumber(1L);
        entity.setStatus(status);
        entity.setTotalBudgetAmount(BigDecimal.ZERO);
        return entity;
    }

    // ─────────────── findAll ───────────────
    @Test
    @DisplayName("findAll deve retornar lista de ordens de domínio")
    void findAll_ShouldReturnList() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);

        when(repository.findAll()).thenReturn(List.of(entity));

        List<ServiceOrder> result = service.findAll();

        assertThat(result).hasSize(1).contains(entity);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("findAll deve retornar lista vazia quando não há ordens")
    void findAll_ShouldReturnEmptyList_WhenNoOrders() {
        when(repository.findAll()).thenReturn(List.of());

        List<ServiceOrder> result = service.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll deve filtrar e ordenar por prioridade do status e data de criação")
    void findAll_ShouldFilterAndSortByStatusAndCreatedAt() {
        ServiceOrder recebidaMaisNova = ServiceOrder.builder()
                .id("1")
                .status(ServiceOrderStatus.RECEBIDA)
                .createdAt(LocalDateTime.of(2025, 1, 20, 10, 0))
                .build();

        ServiceOrder recebidaMaisAntiga = ServiceOrder.builder()
                .id("2")
                .status(ServiceOrderStatus.RECEBIDA)
                .createdAt(LocalDateTime.of(2025, 1, 10, 10, 0))
                .build();

        ServiceOrder emDiagnostico = ServiceOrder.builder()
                .id("3")
                .status(ServiceOrderStatus.EM_DIAGNOSTICO)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        ServiceOrder aguardandoAprovacao = ServiceOrder.builder()
                .id("4")
                .status(ServiceOrderStatus.AGUARDANDO_APROVACAO)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        ServiceOrder emExecucao = ServiceOrder.builder()
                .id("5")
                .status(ServiceOrderStatus.EM_EXECUCAO)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        when(repository.findAll()).thenReturn(List.of(
                recebidaMaisNova,
                emDiagnostico,
                aguardandoAprovacao,
                recebidaMaisAntiga,
                emExecucao
        ));

        List<ServiceOrder> result = service.findAll();

        assertThat(result).containsExactly(
                emExecucao,
                aguardandoAprovacao,
                emDiagnostico,
                recebidaMaisAntiga,
                recebidaMaisNova
        );
    }

    @Test
    @DisplayName("findAll deve ignorar ordens com status não permitidos")
    void findAll_ShouldFilterUnsupportedStatuses() {
        ServiceOrder recebida = buildEntity("1", ServiceOrderStatus.RECEBIDA);
        ServiceOrder finalizada = buildEntity("2", ServiceOrderStatus.FINALIZADA);

        when(repository.findAll()).thenReturn(List.of(recebida, finalizada));

        List<ServiceOrder> result = service.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(recebida);
    }

    // ─────────────── findById ───────────────

    @Test
    @DisplayName("findById deve retornar a ordem quando encontrada")
    void findById_ShouldReturnOrder_WhenFound() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        ServiceOrder result = service.findById("id-1");

        assertThat(result).isEqualTo(entity);
    }

    @Test
    @DisplayName("findById deve lançar NotFoundException quando não encontrado")
    void findById_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    // ─────────────── findByServiceOrderNumber ───────────────

    @Test
    @DisplayName("findByServiceOrderNumber deve retornar a ordem quando encontrada")
    void findByServiceOrderNumber_ShouldReturnOrder_WhenFound() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);

        when(repository.findByServiceOrderNumber(100L)).thenReturn(Optional.of(entity));

        ServiceOrder result = service.findByServiceOrderNumber(100L);

        assertThat(result).isEqualTo(entity);
    }

    @Test
    @DisplayName("findByServiceOrderNumber deve lançar NotFoundException quando não encontrado")
    void findByServiceOrderNumber_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findByServiceOrderNumber(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByServiceOrderNumber(999L))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    // ─────────────── createServiceOrder ───────────────

    @Test
    @DisplayName("createServiceOrder deve criar e retornar nova ordem de serviço")
    void createServiceOrder_ShouldCreateAndReturnOrder() {
        NewServiceOrderRequest request = NewServiceOrderRequest.builder()
                .customerId("cust-1")
                .vehicleId("veh-1")
                .laborIds(List.of(new LaborsRequest("lab-1")))
                .informationText("Diagnóstico inicial")
                .build();

        LaborDetailDTO laborSnapshot = LaborDetailDTO.builder()
                .laborId("lab-1")
                .name("Troca de Óleo")
                .laborPrice(new BigDecimal("100.00"))
                .situation(LaborSituation.PENDENTE)
                .build();
        CustomerDTO customer = new CustomerDTO();
        VehicleDTO vehicle = new VehicleDTO();
        ServiceOrder entity = buildEntity(null, null);

        when(laborSelectionService.getLaborSnapshot("lab-1")).thenReturn(laborSnapshot);
        when(customerAndMecnichalService.getCustomer("cust-1")).thenReturn(customer);
        when(vehicleSelectionService.getVehicle("veh-1")).thenReturn(vehicle);
        when(mapper.toCreateEntity(any(), any(), any(), any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.createServiceOrder(request);

        assertThat(result).isEqualTo(entity);
        verify(laborSelectionService).getLaborSnapshot("lab-1");
        verify(repository).save(entity);
        verify(eventPublisherGateway).publishForDiagnostics(entity.getId());
    }

    // ─────────────── updateServiceOrder ───────────────

    @Test
    @DisplayName("updateServiceOrder deve atualizar com mecânico quando mechanicId fornecido")
    void updateServiceOrder_ShouldUpdateWithMechanic_WhenMechanicIdProvided() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        ExistServiceOrderRequest request = new ExistServiceOrderRequest("Observação", "mech-1");
        MechanicDTO mechanic = new MechanicDTO("mech-1", "Carlos");

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(customerAndMecnichalService.getMechanic("mech-1")).thenReturn(mechanic);
        when(mapper.toUpdateEntity(request, entity, mechanic)).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.updateServiceOrder("id-1", request);

        assertThat(result).isEqualTo(entity);
        verify(customerAndMecnichalService).getMechanic("mech-1");
    }

    @Test
    @DisplayName("updateServiceOrder deve atualizar sem mecânico quando mechanicId for nulo")
    void updateServiceOrder_ShouldUpdateWithoutMechanic_WhenMechanicIdIsNull() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        ExistServiceOrderRequest request = new ExistServiceOrderRequest("Observação", null);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toUpdateEntity(request, entity, null)).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);

        service.updateServiceOrder("id-1", request);

        verify(customerAndMecnichalService, never()).getMechanic(any());
    }

    // ─────────────── deleteServiceOrder ───────────────

    @Test
    @DisplayName("deleteServiceOrder deve invocar deleteById no repositório")
    void deleteServiceOrder_ShouldCallRepository() {
        doNothing().when(repository).deleteById("id-1");

        service.deleteServiceOrder("id-1");

        verify(repository).deleteById("id-1");
    }

    // ─────────────── addLaborsInServiceOrder ───────────────

    @Test
    @DisplayName("addLaborsInServiceOrder deve adicionar serviços e salvar")
    void addLaborsInServiceOrder_ShouldAddLaborsAndSave() {
        LaborsDTO existingLabors = new LaborsDTO(new ArrayList<>(), BigDecimal.ZERO);
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        entity.setLabors(existingLabors);

        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(laborSelectionService.getLaborSnapshot("lab-1")).thenReturn(labor);
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.addLaborsInServiceOrder("id-1", List.of(new LaborsRequest("lab-1")));

        assertThat(result).isEqualTo(entity);
        assertThat(result.getLabors().getLaborsDetails()).hasSize(1);
        assertThat(result.getLabors().getTotalLaborsAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }

    // ─────────────── removeLaborFromServiceOrder ───────────────

    @Test
    @DisplayName("removeLaborFromServiceOrder deve remover serviço pelo laborId e salvar")
    void removeLaborFromServiceOrder_ShouldRemoveLaborAndSave() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.removeLaborFromServiceOrder("id-1", "lab-1");

        assertThat(entity.getLabors().getLaborsDetails()).isEmpty();
        verify(repository).save(entity);
    }

    // ─────────────── addSupplyFromServiceOrder ───────────────

    @Test
    @DisplayName("addSupplyFromServiceOrder deve adicionar suprimentos e salvar")
    void addSupplyFromServiceOrder_ShouldAddSuppliesAndSave() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        entity.setSupplys(null);

        SupplyDTO supply = new SupplyDTO(List.of(), BigDecimal.ZERO);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.addSupplyFromServiceOrder("id-1", List.of(new SupplysRequest("sup-1", 2)));

        assertThat(result).isEqualTo(entity);
        assertThat(result.getSupplys().getSupplysDetails()).hasSize(1);
    }

    @Test
    @DisplayName("addSupplyFromServiceOrder deve calcular preco unitario, total e totalSupplyAmount")
    void addSupplyFromServiceOrder_ShouldCalculateAmountsCorrectly() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        entity.setSupplys(null);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        SupplysRequest request = SupplysRequest.builder()
                .id("sup-1")
                .name("Oleo")
                .description("Oleo 5w30")
                .quantity(3)
                .unitPrice(new BigDecimal("50.00"))
                .build();

        ServiceOrder result = service.addSupplyFromServiceOrder("id-1", List.of(request));

        assertThat(result.getSupplys().getSupplysDetails()).hasSize(1);
        SupplyDetailDTO detail = result.getSupplys().getSupplysDetails().get(0);
        assertThat(detail.getName()).isEqualTo("Oleo");
        assertThat(detail.getDescription()).isEqualTo("Oleo 5w30");
        assertThat(detail.getQuantity()).isEqualTo(3);
        assertThat(detail.getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(detail.getTotalPrice()).isEqualByComparingTo("150.00");
        assertThat(result.getSupplys().getTotalSupplyAmount()).isEqualByComparingTo("150.00");
    }

    // ─────────────── removeSupplyFromServiceOrder ───────────────

    @Test
    @DisplayName("removeSupplyFromServiceOrder deve remover suprimento e salvar")
    void removeSupplyFromServiceOrder_ShouldRemoveSupplyAndSave() {
        SupplyDetailDTO supply = SupplyDetailDTO.builder().id("sup-1").build();
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        entity.setSupplys(new SupplyDTO(new ArrayList<>(List.of(supply)), BigDecimal.ZERO));

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.removeSupplyFromServiceOrder("id-1", "sup-1");

        assertThat(entity.getSupplys().getSupplysDetails()).isEmpty();
        verify(repository).save(entity);
    }

    // ─────────────── updateStatus ───────────────

    @Test
    @DisplayName("Deve atualizar status com sucesso quando a precedência for respeitada")
    void updateStatus_ShouldSuccess_WhenTransitionIsValid() {
        ServiceOrder entity = buildEntity("123", ServiceOrderStatus.RECEBIDA);

        when(repository.findById("123")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.updateStatus("123", ServiceOrderStatus.EM_DIAGNOSTICO);

        assertThat(entity.getStatus()).isEqualTo(ServiceOrderStatus.EM_DIAGNOSTICO);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar pular um status na precedência")
    void updateStatus_ShouldThrowException_WhenTransitionIsInvalid() {
        ServiceOrder entity = buildEntity("123", ServiceOrderStatus.RECEBIDA);

        when(repository.findById("123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus("123", ServiceOrderStatus.APROVADA))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("Apenas ordens AGUARDANDO APROVAÇÃO podem ser aprovadas.");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar retornar para o status RECEBIDA")
    void updateStatus_ShouldThrowException_WhenReturningToReceived() {
        ServiceOrder entity = buildEntity("123", ServiceOrderStatus.EM_DIAGNOSTICO);

        when(repository.findById("123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus("123", ServiceOrderStatus.RECEBIDA))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("A Ordem de Serviço já foi recebida e não pode retornar a este status.");
    }

    @Test
    @DisplayName("Deve lançar exceção se o novo status for igual ao atual")
    void updateStatus_ShouldThrowException_WhenStatusIsSame() {
        ServiceOrder entity = buildEntity("123", ServiceOrderStatus.EM_DIAGNOSTICO);

        when(repository.findById("123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus("123", ServiceOrderStatus.EM_DIAGNOSTICO))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("A Ordem de Serviço já foi processada com status " + ServiceOrderStatus.EM_DIAGNOSTICO.getStatusName() + ".");
    }

    @Test
    @DisplayName("Deve validar transição para ENTREGUE apenas após FINALIZADA")
    void updateStatus_ShouldValidateDelivery_OnlyAfterFinalized() {
        ServiceOrder entity = buildEntity("123", ServiceOrderStatus.EM_EXECUCAO);

        when(repository.findById("123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus("123", ServiceOrderStatus.ENTREGUE))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("Apenas ordens FINALIZADAS podem ser consideradas entregues");
    }

    // ─────────────── startLabor ───────────────

    @Test
    @DisplayName("startLabor deve iniciar serviço e mudar status para EM_EXECUCAO quando APROVADA")
    void startLabor_ShouldStart_AndTransitionToEmExecucao() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(null);

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.APROVADA);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.startLabor("id-1", "lab-1");

        assertThat(labor.getStartDate()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(ServiceOrderStatus.EM_EXECUCAO);
    }

    @Test
    @DisplayName("startLabor deve lançar DomainException quando serviço já iniciado")
    void startLabor_ShouldThrow_WhenLaborAlreadyStarted() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(LocalDateTime.now());

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.EM_EXECUCAO);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.startLabor("id-1", "lab-1"))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("O serviço já foi iniciado");
    }

    @Test
    @DisplayName("startLabor deve lançar NotFoundException quando serviço não pertence à OS")
    void startLabor_ShouldThrow_WhenLaborNotFound() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(null);

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.APROVADA);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.startLabor("id-1", "lab-inexistente"))
                .isInstanceOf(ServiceOrderNotFoundException.class)
                .hasMessageContaining("A O.S não possui este serviço");
    }

    @Test
    @DisplayName("startLabor deve lançar DomainException quando status da OS não permite execução")
    void startLabor_ShouldThrow_WhenStatusNotAllowed() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        entity.setLabors(new LaborsDTO(new ArrayList<>(List.of(labor)), BigDecimal.ZERO));

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.startLabor("id-1", "lab-1"))
                .isInstanceOf(ServiceOrderBusinessException.class);
    }

    // ─────────────── finishLabor ───────────────

    @Test
    @DisplayName("finishLabor deve finalizar serviço e registrar endDate")
    void finishLabor_ShouldFinish_AndRegisterEndDate() {
        LocalDateTime start = LocalDateTime.now().minusHours(4);
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(start);
        labor.setEndDate(null);

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.EM_EXECUCAO);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.finishLabor("id-1", "lab-1");

        assertThat(labor.getEndDate()).isNotNull();
    }

    @Test
    @DisplayName("finishLabor deve lançar DomainException quando serviço não foi iniciado")
    void finishLabor_ShouldThrow_WhenLaborNotStarted() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(null);
        labor.setEndDate(null);

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.EM_EXECUCAO);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.finishLabor("id-1", "lab-1"))
                .isInstanceOf(ServiceOrderBusinessException.class)
                .hasMessageContaining("Não é possível finalizar um serviço que não foi iniciado ou já foi finalizado.");
    }

    @Test
    @DisplayName("finishLabor deve lançar NotFoundException quando serviço não pertence à OS")
    void finishLabor_ShouldThrow_WhenLaborNotFound() {
        LaborDetailDTO labor = new LaborDetailDTO("lab-1", "Serviço", "Desc", BigDecimal.TEN, null, null, LaborSituation.PENDENTE, LocalDateTime.now());
        labor.setStartDate(LocalDateTime.now().minusHours(2));
        labor.setEndDate(null);

        List<LaborDetailDTO> details = new ArrayList<>(List.of(labor));
        LaborsDTO labors = new LaborsDTO(details, BigDecimal.TEN);

        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.EM_EXECUCAO);
        entity.setLabors(labors);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.finishLabor("id-1", "lab-inexistente"))
                .isInstanceOf(ServiceOrderNotFoundException.class)
                .hasMessageContaining("A O.S não possui este serviço");
    }

    // ─────────────── sendToCustomer ───────────────

    @Test
    @DisplayName("sendToCustomer deve mudar status para AGUARDANDO_APROVACAO e salvar")
    void sendToCustomer_ShouldTransitionToAguardandoAprovacao() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.EM_DIAGNOSTICO);

        when(repository.findById("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.sendToCustomer("id-1");

        assertThat(entity.getStatus()).isEqualTo(ServiceOrderStatus.AGUARDANDO_APROVACAO);
        verify(repository).save(entity);
    }

    // ─────────────── save ───────────────

    @Test
    @DisplayName("save deve calcular orçamento e persistir entidade")
    void save_ShouldCalculateBudgetAndPersist() {
        ServiceOrder entity = buildEntity("id-1", ServiceOrderStatus.RECEBIDA);

        when(repository.save(entity)).thenReturn(entity);

        ServiceOrder result = service.save(entity);

        assertThat(result).isEqualTo(entity);
        verify(repository).save(entity);
    }

    // ─────────────── startDiagnosis ───────────────

    @Test
    @DisplayName("startDiagnosis deve transicionar para EM_DIAGNOSTICO e associar mecanico")
    void startDiagnosis_ShouldTransitionToEmDiagnosticoAndAssignMechanic() {
        ServiceOrder entity = buildEntity("so-1", ServiceOrderStatus.RECEBIDA);
        MechanicDTO mechanic = new MechanicDTO("mech-1", "Carlos");

        when(repository.findById("so-1")).thenReturn(Optional.of(entity));
        when(customerAndMecnichalService.getMechanic("mech-1")).thenReturn(mechanic);
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.startDiagnosis("so-1", "mech-1");

        assertThat(result.getStatus()).isEqualTo(ServiceOrderStatus.EM_DIAGNOSTICO);
        assertThat(result.getMechanic()).isEqualTo(mechanic);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("startDiagnosis sem mecanico deve apenas transicionar status")
    void startDiagnosis_WithoutMechanic_ShouldOnlyTransitionStatus() {
        ServiceOrder entity = buildEntity("so-1", ServiceOrderStatus.RECEBIDA);

        when(repository.findById("so-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.startDiagnosis("so-1", null);

        assertThat(result.getStatus()).isEqualTo(ServiceOrderStatus.EM_DIAGNOSTICO);
        verify(customerAndMecnichalService, never()).getMechanic(any());
        verify(repository).save(entity);
    }

    // ─────────────── applyBudget ───────────────

    @Test
    @DisplayName("applyBudget deve atualizar valores e transicionar para AGUARDANDO_APROVACAO")
    void applyBudget_ShouldSetAmountsAndTransitionToAguardandoAprovacao() {
        ServiceOrder entity = buildEntity("so-1", ServiceOrderStatus.EM_DIAGNOSTICO);
        entity.setLabors(new LaborsDTO(new ArrayList<>(), BigDecimal.ZERO));
        entity.setSupplys(new SupplyDTO(new ArrayList<>(), BigDecimal.ZERO));

        when(repository.findById("so-1")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        ServiceOrder result = service.applyBudget(
                "so-1",
                new BigDecimal("200.00"),
                new BigDecimal("150.00"),
                new BigDecimal("350.00")
        );

        assertThat(result.getStatus()).isEqualTo(ServiceOrderStatus.AGUARDANDO_APROVACAO);
        assertThat(result.getTotalBudgetAmount()).isEqualTo(new BigDecimal("350.00"));
        assertThat(result.getLabors().getTotalLaborsAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(result.getSupplys().getTotalSupplyAmount()).isEqualTo(new BigDecimal("150.00"));
        verify(repository).save(entity);
    }

    // ─────────────── handleDiagnosisCompleted ───────────────

    @Test
    @DisplayName("handleDiagnosisCompleted deve publicar eventGeneratePayment para serviços e suprimentos válidos")
    void handleDiagnosisCompleted_ShouldPublishGeneratePayment() {
        ServiceOrder entity = buildEntity("so-1", ServiceOrderStatus.EM_DIAGNOSTICO);

        LaborDetailDTO labor1 = LaborDetailDTO.builder().laborId("l-1").name("Troca").situation(LaborSituation.PENDENTE).build();
        LaborDetailDTO labor2 = LaborDetailDTO.builder().laborId("l-2").name("Alinhamento").situation(LaborSituation.REJEITADO).build();
        entity.setLabors(new LaborsDTO(List.of(labor1, labor2), new BigDecimal("100.00")));

        SupplyDetailDTO supply = SupplyDetailDTO.builder().id("s-1").name("Filtro").build();
        entity.setSupplys(new SupplyDTO(List.of(supply), new BigDecimal("50.00")));

        when(repository.findById("so-1")).thenReturn(Optional.of(entity));

        service.handleDiagnosisCompleted("so-1");

        verify(eventPublisherGateway).publishGeneratePayment(
                eq("so-1"),
                argThat(list -> list.size() == 1 && list.get(0).getLaborId().equals("l-1")),
                argThat(list -> list.size() == 1 && list.get(0).getId().equals("s-1"))
        );
    }

    // ─────────────── generatePaymentForServiceOrder ───────────────

    @Test
    @DisplayName("generatePaymentForServiceOrder deve publicar apenas serviços com situation APROVADO")
    void generatePaymentForServiceOrder_ShouldPublishOnlyApprovedLabors() {
        ServiceOrder entity = buildEntity("so-2", ServiceOrderStatus.AGUARDANDO_APROVACAO);

        LaborDetailDTO labor1 = LaborDetailDTO.builder().laborId("l-1").situation(LaborSituation.APROVADO).build();
        LaborDetailDTO labor2 = LaborDetailDTO.builder().laborId("l-2").situation(LaborSituation.REJEITADO).build();
        entity.setLabors(new LaborsDTO(List.of(labor1, labor2), new BigDecimal("100.00")));

        when(repository.findById("so-2")).thenReturn(Optional.of(entity));

        service.generatePaymentForServiceOrder("so-2");

        verify(eventPublisherGateway).publishGeneratePayment(
                eq("so-2"),
                argThat(list -> list.size() == 1 && list.get(0).getLaborId().equals("l-1")),
                argThat(List::isEmpty)
        );
    }
}
