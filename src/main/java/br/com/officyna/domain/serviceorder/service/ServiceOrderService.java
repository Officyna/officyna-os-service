package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.api.serviceorder.resources.*;
import br.com.officyna.domain.serviceorder.dto.*;
import br.com.officyna.domain.serviceorder.entity.ServiceOrder;
import br.com.officyna.domain.serviceorder.enums.LaborSituation;
import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderBusinessException;
import br.com.officyna.domain.serviceorder.exception.ServiceOrderNotFoundException;
import br.com.officyna.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.officyna.domain.serviceorder.repository.ServiceOrderRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ServiceOrderService {

    private final ServiceOrderRepository repository;
    private final CustomerAndMecnichalService customerAndMecnichalService;
    private final VehicleSelectionService vehicleSelectionService;
    private final ServiceOrderMapper mapper;

    public ServiceOrderService(ServiceOrderRepository repository,
                               CustomerAndMecnichalService customerAndMecnichalService,
                               VehicleSelectionService vehicleSelectionService,
                               ServiceOrderMapper mapper) {
        this.repository = repository;
        this.customerAndMecnichalService = customerAndMecnichalService;
        this.vehicleSelectionService = vehicleSelectionService;
        this.mapper = mapper;
    }

    private ServiceOrder findEntityById(String id) {
        log.info("Finding service order by id: {}", id);

        ServiceOrder serviceOrder = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Service order not found by id: {}", id);
                    return ServiceOrderNotFoundException.of(id);
                });

        log.debug("Service order found by id: {}", id);

        return serviceOrder;
    }

    public List<ServiceOrder> findAll() {
        log.info("Finding all service orders");

        List<ServiceOrder> ordersServiceEntity = repository.findAll();

        log.debug("Found {} service orders", ordersServiceEntity.size());

        List<ServiceOrder> sortedOrders = sortOrdersServiceByStatusAndDate(ordersServiceEntity);

        log.debug("Service orders sorted successfully. Total orders returned: {}", sortedOrders.size());

        return sortedOrders;
    }

    private List<ServiceOrder> sortOrdersServiceByStatusAndDate(
            List<ServiceOrder> ordersServiceEntity) {

        log.debug("Sorting service orders by status priority and creation date");

        return ordersServiceEntity.stream()
                .filter(order ->
                        order.getStatus() == ServiceOrderStatus.EM_EXECUCAO
                                || order.getStatus() == ServiceOrderStatus.AGUARDANDO_APROVACAO
                                || order.getStatus() == ServiceOrderStatus.APROVADA
                                || order.getStatus() == ServiceOrderStatus.EM_DIAGNOSTICO
                                || order.getStatus() == ServiceOrderStatus.RECEBIDA)
                .sorted(
                        Comparator
                                .comparingInt((ServiceOrder order) ->
                                        order.getStatus().getPriority())
                                .thenComparing(ServiceOrder::getCreatedAt)
                )
                .toList();
    }

    public ServiceOrder findById(String id) {
        log.info("Finding service order by id: {}", id);

        ServiceOrder serviceOrder = this.findEntityById(id);

        log.debug("Service order retrieved successfully by id: {}", id);

        return serviceOrder;
    }

    public ServiceOrder findByServiceOrderNumber(Long serviceOrderNumber) {
        log.info("Finding service order by number: {}", serviceOrderNumber);

        ServiceOrder serviceOrder = repository.findByServiceOrderNumber(serviceOrderNumber)
                .orElseThrow(() -> {
                    log.warn("Service order not found by number: {}", serviceOrderNumber);
                    return ServiceOrderNotFoundException.of(serviceOrderNumber);
                });

        log.debug("Service order found by number: {}", serviceOrderNumber);

        return serviceOrder;
    }

    public ServiceOrder createServiceOrder(NewServiceOrderRequest request) {
        log.info("Creating new service order for customer id: {}", request.getCustomerId());

        LaborsDTO labors = (request.getLaborIds() != null && !request.getLaborIds().isEmpty())
                ? new LaborsDTO(
                        request.getLaborIds().stream()
                                .map(l -> LaborDetailDTO.builder()
                                        .laborId(l.getId())
                                        .situation(LaborSituation.PENDENTE)
                                        .build())
                                .collect(Collectors.toList()),
                        BigDecimal.ZERO
                )
                : new LaborsDTO(new ArrayList<>(), BigDecimal.ZERO);

        log.debug("Labors selected successfully for customer id: {}", request.getCustomerId());

        CustomerDTO customer = customerAndMecnichalService.getCustomer(
                request.getCustomerId()
        );

        log.debug("Customer retrieved successfully. Customer id: {}", request.getCustomerId());

        VehicleDTO vehicle = vehicleSelectionService.getVehicle(
                request.getVehicleId()
        );

        log.debug("Vehicle retrieved successfully. Vehicle id: {}", request.getVehicleId());

        ServiceOrder entity = mapper.toCreateEntity(
                request,
                vehicle,
                customer,
                labors
        );

        entity.setStatus(ServiceOrderStatus.RECEBIDA);

        ServiceOrder saved = this.save(entity);

        log.info(
                "Service order created. event=SERVICE_ORDER_CREATED, serviceOrderId={}, serviceOrderNumber={}, status={}",
                saved.getId(),
                saved.getServiceOrderNumber(),
                saved.getStatus()
        );

        return saved;
    }

    public ServiceOrder updateServiceOrder(
            String id,
            ExistServiceOrderRequest request) {

        log.info("Updating service order by id: {}", id);

        ServiceOrder entity = this.findEntityById(id);

        MechanicDTO mechanic = (
                request.getMechanicId() == null
                        || request.getMechanicId().isEmpty()
        )
                ? null
                : customerAndMecnichalService.getMechanic(
                request.getMechanicId()
        );

        log.debug("Mechanic information processed for service order id: {}", id);

        ServiceOrder updated = this.save(
                mapper.toUpdateEntity(request, entity, mechanic)
        );

        log.info("Service order updated successfully. Id: {}", id);

        return updated;
    }

    public void deleteServiceOrder(String id) {
        log.warn("Deleting service order by id: {}", id);

        repository.deleteById(id);

        log.info("Service order deleted successfully. Id: {}", id);
    }

    public ServiceOrder addLaborsInServiceOrder(
            String id,
            List<LaborsRequest> laborsIdList) {

        log.info(
                "Adding {} labor(s) to service order id: {}",
                laborsIdList != null ? laborsIdList.size() : 0,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        List<LaborDetailDTO> current = (entity.getLabors() != null && entity.getLabors().getLaborsDetails() != null)
                ? new ArrayList<>(entity.getLabors().getLaborsDetails())
                : new ArrayList<>();

        if (laborsIdList != null) {
            for (LaborsRequest req : laborsIdList) {
                current.add(LaborDetailDTO.builder()
                        .laborId(req.getId())
                        .situation(LaborSituation.PENDENTE)
                        .build());
            }
        }

        entity.setLabors(new LaborsDTO(current, BigDecimal.ZERO));

        ServiceOrder saved = this.save(entity);

        log.info(
                "Labor(s) added successfully to service order id: {}",
                id
        );

        return saved;
    }

    public ServiceOrder removeLaborFromServiceOrder(
            String id,
            String laborId) {

        log.info(
                "Removing labor id: {} from service order id: {}",
                laborId,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        if (entity.getLabors() != null && entity.getLabors().getLaborsDetails() != null) {
            List<LaborDetailDTO> laborsDetails = new ArrayList<>(entity.getLabors().getLaborsDetails());
            laborsDetails.removeIf(labor -> labor.getLaborId().equals(laborId));
            entity.setLabors(new LaborsDTO(laborsDetails, BigDecimal.ZERO));
        }

        ServiceOrder saved = this.save(entity);

        log.info(
                "Labor removed successfully. Labor id: {}, Service order id: {}",
                laborId,
                id
        );

        return saved;
    }

    public ServiceOrder addSupplyFromServiceOrder(
            String id,
            List<SupplysRequest> supplyIdList) {

        log.info(
                "Adding {} supply(s) to service order id: {}",
                supplyIdList != null ? supplyIdList.size() : 0,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        List<SupplyDetailDTO> current = (entity.getSupplys() != null && entity.getSupplys().getSupplysDetails() != null)
                ? new ArrayList<>(entity.getSupplys().getSupplysDetails())
                : new ArrayList<>();

        if (supplyIdList != null) {
            for (SupplysRequest req : supplyIdList) {
                current.add(SupplyDetailDTO.builder()
                        .id(req.getId())
                        .quantity(req.getQuantity() != null ? req.getQuantity() : 1)
                        .unitPrice(BigDecimal.ZERO)
                        .totalPrice(BigDecimal.ZERO)
                        .build());
            }
        }

        entity.setSupplys(new SupplyDTO(current, BigDecimal.ZERO));

        ServiceOrder saved = this.save(entity);

        log.info(
                "Supply(s) added successfully to service order id: {}",
                id
        );

        return saved;
    }

    public ServiceOrder removeSupplyFromServiceOrder(
            String id,
            String supplyId) {

        log.info(
                "Removing supply id: {} from service order id: {}",
                supplyId,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        if (entity.getSupplys() != null && entity.getSupplys().getSupplysDetails() != null) {
            List<SupplyDetailDTO> current = new ArrayList<>(entity.getSupplys().getSupplysDetails());
            current.removeIf(supply -> supply.getId().equals(supplyId));
            entity.setSupplys(new SupplyDTO(current, BigDecimal.ZERO));
        }

        ServiceOrder saved = this.save(entity);

        log.info(
                "Supply removed successfully. Supply id: {}, Service order id: {}",
                supplyId,
                id
        );

        return saved;
    }

    public ServiceOrder updateStatus(
            String id,
            ServiceOrderStatus status) {

        log.info(
                "Updating service order status. Id: {}, New status: {}",
                id,
                status
        );

        ServiceOrder entity = this.findEntityById(id);

        ServiceOrderStatus previousStatus = entity.getStatus();

        entity.setStatus(status);

        logStatusChange(
                entity,
                previousStatus,
                status
        );

        ServiceOrder saved = this.save(entity);

        log.info(
                "Service order status updated successfully. Id: {}, Status: {}",
                id,
                status
        );

        return saved;
    }

    public ServiceOrder startLabor(
            String id,
            String laborId) {

        log.info(
                "Starting labor execution. Labor id: {}, Service order id: {}",
                laborId,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        this.validateStatusForStartExecution(entity);

        boolean found = false;

        for (LaborDetailDTO labor : entity.getLabors().getLaborsDetails()) {

            if (labor.getLaborId().equals(laborId)) {

                if (labor.getStartDate() == null) {

                    labor.setStartDate(LocalDateTime.now());

                    found = true;

                    log.debug(
                            "Labor execution started. Labor id: {}, Service order id: {}",
                            laborId,
                            id
                    );

                    break;

                } else {

                    log.error(
                            "Attempt to start an already started labor. Service order id: {}, Labor id: {}",
                            id,
                            laborId
                    );

                    throw new ServiceOrderBusinessException(
                            "O serviço já foi iniciado"
                    );
                }
            }
        }

        if (!found) {

            log.error(
                    "Labor not found in service order. Labor id: {}, Service order id: {}",
                    laborId,
                    id
            );

            throw new ServiceOrderNotFoundException(
                    "A O.S não possui este serviço"
            );
        }

        if (entity.getStatus().equals(ServiceOrderStatus.APROVADA)) {

            ServiceOrderStatus previousStatus = entity.getStatus();

            entity.setStatus(ServiceOrderStatus.EM_EXECUCAO);

            logStatusChange(
                    entity,
                    previousStatus,
                    ServiceOrderStatus.EM_EXECUCAO
            );
        }

        ServiceOrder saved = this.save(entity);

        log.info(
                "Labor execution started successfully. Labor id: {}, Service order id: {}",
                laborId,
                id
        );

        return saved;
    }

    public ServiceOrder finishLabor(
            String id,
            String laborId) {

        log.info(
                "Finishing labor execution. Labor id: {}, Service order id: {}",
                laborId,
                id
        );

        ServiceOrder entity = this.findEntityById(id);

        this.validateStatusForStartExecution(entity);

        boolean found = false;

        for (LaborDetailDTO labor : entity.getLabors().getLaborsDetails()) {

            if (labor.getLaborId().equals(laborId)) {

                if (labor.getEndDate() == null
                        && labor.getStartDate() != null) {

                    found = true;

                    labor.setEndDate(LocalDateTime.now());

                    log.debug(
                            "Updating labor execution time. Labor id: {}, Service order id: {}",
                            laborId,
                            id
                    );

                    break;

                } else {

                    log.error(
                            "Failed to finish labor. Labor was not started or was already finished. Service order id: {}, Labor id: {}",
                            id,
                            laborId
                    );

                    throw new ServiceOrderBusinessException(
                            "Não é possível finalizar um serviço que não foi iniciado ou já foi finalizado."
                    );
                }
            }
        }

        if (!found) {

            log.error(
                    "Labor not found in service order. Labor id: {}, Service order id: {}",
                    laborId,
                    id
            );

            throw new ServiceOrderNotFoundException(
                    "A O.S não possui este serviço"
            );
        }

        ServiceOrder saved = this.save(entity);

        log.info(
                "Labor execution finished successfully. Labor id: {}, Service order id: {}",
                laborId,
                id
        );

        return saved;
    }

    public ServiceOrder save(ServiceOrder entity) {

        log.debug(
                "Saving service order. Id: {}",
                entity.getId()
        );

        ServiceOrder saved = repository.save(entity);

        log.debug(
                "Service order saved successfully. Id: {}",
                saved.getId()
        );

        return saved;
    }

    public void sendToCustomer(String id) {

        log.info(
                "Sending service order to customer for approval. Id: {}",
                id
        );

        ServiceOrder serviceOrder = this.findEntityById(id);

        ServiceOrderStatus previousStatus = serviceOrder.getStatus();

        serviceOrder.setStatus(
                ServiceOrderStatus.AGUARDANDO_APROVACAO
        );

        logStatusChange(
                serviceOrder,
                previousStatus,
                ServiceOrderStatus.AGUARDANDO_APROVACAO
        );

        repository.save(serviceOrder);

        log.info(
                "Service order sent to customer successfully. Id: {}",
                id
        );
    }

    private void validateStatusForStartExecution(ServiceOrder entity) {

        log.debug(
                "Validating service order status for labor execution. Id: {}, Status: {}",
                entity.getId(),
                entity.getStatus()
        );

        if (!(ServiceOrderStatus.APROVADA.equals(entity.getStatus())
                || ServiceOrderStatus.EM_EXECUCAO.equals(entity.getStatus()))) {

            log.warn(
                    "Invalid service order status for labor execution. Id: {}, Current status: {}",
                    entity.getId(),
                    entity.getStatus()
                );

            throw new ServiceOrderBusinessException(
                    "Um serviço só pode ser iniciado ou finalizado se o status da ordem de serviço for APROVADA ou EM EXECUÇÃO."
            );
        }

        LaborsDTO labors = entity.getLabors();

        if (labors == null
                || labors.getLaborsDetails() == null
                || labors.getLaborsDetails().isEmpty()) {

            log.warn(
                    "Service order has no labors registered. Id: {}",
                    entity.getId()
            );

            throw new ServiceOrderBusinessException(
                    "A ordem de serviço não possui serviços cadastrados."
            );
        }

        log.debug(
                "Service order validation completed successfully. Id: {}",
                entity.getId()
        );
    }

    public void logStatusChange(
            ServiceOrder entity,
            ServiceOrderStatus previousStatus,
            ServiceOrderStatus newStatus) {

        log.info(
                "Service order status changed. event=SERVICE_ORDER_STATUS_CHANGED, " +
                        "serviceOrderId={}, previousStatus={}, status={}",
                entity.getId(),
                previousStatus,
                newStatus
        );

        LocalDateTime statusStartDate = getStatusStartDate(
                entity,
                previousStatus
        );

        LocalDateTime statusEndDate = getStatusStartDate(
                entity,
                newStatus
        );

        if (statusStartDate != null && statusEndDate != null) {

            long durationInSeconds =
                    java.time.Duration.between(
                            statusStartDate,
                            statusEndDate
                    ).getSeconds();

            double durationInMinutes =
                    durationInSeconds / 60.0;

            log.info(
                    "Service order status duration. " +
                            "event=SERVICE_ORDER_STATUS_DURATION, " +
                            "serviceOrderId={}, " +
                            "status={}, " +
                            "startDate={}, " +
                            "endDate={}, " +
                            "durationInSeconds={}, " +
                            "durationInMinutes={}",
                    entity.getId(),
                    previousStatus,
                    statusStartDate,
                    statusEndDate,
                    durationInSeconds,
                    durationInMinutes
            );
        }
    }

    private LocalDateTime getStatusStartDate(
            ServiceOrder entity,
            ServiceOrderStatus status) {

        if (status == null) {
            return null;
        }

        return switch (status) {

            case RECEBIDA ->
                    entity.getRegistrationDate();

            case EM_DIAGNOSTICO ->
                    entity.getDiagnosisStartDate();

            case AGUARDANDO_APROVACAO ->
                    entity.getClientSendDate();

            case APROVADA ->
                    entity.getApprovalDate();

            case EM_EXECUCAO ->
                    entity.getExecutionStartDate();

            case FINALIZADA ->
                    entity.getFinalizationDate();

            case ENTREGUE ->
                    entity.getDeliveryDate();

            case RECUSADA ->
                    entity.getRefuseDate();
        };
    }
}