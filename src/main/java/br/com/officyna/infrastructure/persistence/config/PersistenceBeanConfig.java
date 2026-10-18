package br.com.officyna.infrastructure.persistence.config;

import br.com.officyna.domain.customer.controller.CustomerControllerAdapter;
import br.com.officyna.domain.customer.mapper.CustomerMapper;
import br.com.officyna.domain.customer.presenter.CustomerPresenter;
import br.com.officyna.domain.customer.repository.CustomerRepository;
import br.com.officyna.domain.customer.service.CustomerService;
import br.com.officyna.domain.vehicle.controller.VehicleControllerAdapter;
import br.com.officyna.domain.vehicle.mapper.VehicleMapper;
import br.com.officyna.domain.vehicle.presenter.VehiclePresenter;
import br.com.officyna.domain.vehicle.repository.VehicleRepository;
import br.com.officyna.domain.vehicle.service.VehicleService;
import br.com.officyna.domain.serviceorder.controller.CustomerServiceOrderControllerAdapter;
import br.com.officyna.domain.serviceorder.controller.ServiceOrderControllerAdapter;
import br.com.officyna.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.officyna.domain.serviceorder.presenter.ServiceOrderPresenter;
import br.com.officyna.domain.serviceorder.repository.ServiceOrderRepository;
import br.com.officyna.domain.serviceorder.gateway.ProductionLaborGateway;
import br.com.officyna.domain.serviceorder.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceBeanConfig {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public org.springframework.web.client.RestClient.Builder restClientBuilder() {
        return org.springframework.web.client.RestClient.builder();
    }

    @Bean
    public CustomerService customerService(CustomerRepository customerRepository) {
        return new CustomerService(customerRepository);
    }

    @Bean
    public CustomerControllerAdapter customerControllerAdapter(CustomerService customerService,
                                                              CustomerMapper customerMapper,
                                                              CustomerPresenter customerPresenter) {
        return new CustomerControllerAdapter(customerService, customerMapper, customerPresenter);
    }

    @Bean
    public VehicleService vehicleService(VehicleRepository vehicleRepository, CustomerService customerService) {
        return new VehicleService(vehicleRepository, customerService);
    }

    @Bean
    public VehicleControllerAdapter vehicleControllerAdapter(VehicleService vehicleService,
                                                             VehicleMapper vehicleMapper,
                                                             VehiclePresenter vehiclePresenter) {
        return new VehicleControllerAdapter(vehicleService, vehicleMapper, vehiclePresenter);
    }

    @Bean
    public VehicleSelectionService vehicleSelectionService(VehicleService vehicleService) {
        return new VehicleSelectionService(vehicleService);
    }

    @Bean
    public CustomerAndMecnichalService customerAndMecnichalService(CustomerService customerService) {
        return new CustomerAndMecnichalService(customerService);
    }

    @Bean
    public LaborSelectionService laborSelectionService(ProductionLaborGateway productionLaborGateway) {
        return new LaborSelectionService(productionLaborGateway);
    }

    @Bean
    public ServiceOrderService serviceOrderService(ServiceOrderRepository serviceOrderRepository,
                                                   CustomerAndMecnichalService customerAndMecnichalService,
                                                   VehicleSelectionService vehicleSelectionService,
                                                   LaborSelectionService laborSelectionService,
                                                   ServiceOrderMapper mapper) {
        return new ServiceOrderService(
                serviceOrderRepository,
                customerAndMecnichalService,
                vehicleSelectionService,
                laborSelectionService,
                mapper
        );
    }

    @Bean
    public ServiceOrderControllerAdapter serviceOrderControllerAdapter(ServiceOrderService serviceOrderService,
                                                                       ServiceOrderPresenter serviceOrderPresenter) {
        return new ServiceOrderControllerAdapter(serviceOrderService, serviceOrderPresenter);
    }

    @Bean
    public CustomerServiceOrderService customerServiceOrderService(ServiceOrderRepository serviceOrderRepository,
                                                                   CustomerAndMecnichalService customerAndMecnichalService,
                                                                   ServiceOrderService serviceOrderService) {
        return new CustomerServiceOrderService(serviceOrderRepository, customerAndMecnichalService, serviceOrderService);
    }

    @Bean
    public CustomerServiceOrderControllerAdapter customerServiceOrderControllerAdapter(CustomerServiceOrderService customerServiceOrderService,
                                                                                       ServiceOrderPresenter serviceOrderPresenter) {
        return new CustomerServiceOrderControllerAdapter(customerServiceOrderService, serviceOrderPresenter);
    }
}
