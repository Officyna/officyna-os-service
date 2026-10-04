package br.com.officyna.domain.serviceorder.service;

import br.com.officyna.domain.customer.entity.Customer;
import br.com.officyna.domain.customer.service.CustomerService;
import br.com.officyna.domain.serviceorder.dto.CustomerDTO;
import br.com.officyna.domain.serviceorder.dto.MechanicDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomerAndMecnichalService {

    private final CustomerService customerService;

    public CustomerAndMecnichalService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CustomerDTO getCustomer(String id) {
        log.info("Finding customer by id: {}", id);

        Customer customer = customerService.findById(id);

        log.debug("Customer found by id: {}", id);

        var address = customer.getAddress();
        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                address != null ? address.getStreet() : null,
                address != null ? address.getNumber() : null,
                address != null ? address.getNeighborhood() : null,
                address != null ? address.getCity() : null,
                address != null ? address.getState() : null,
                address != null ? address.getZipCode() : null,
                address != null ? address.getComplement() : null
        );
    }

    public Customer getCustomerByDocument(String document) {
        log.info("Finding customer by document: {}", document);

        Customer customer = customerService.findByDocument(document);

        log.debug("Customer found by document: {}", document);

        return customer;
    }

    public MechanicDTO getMechanic(String id) {
        log.info("Resolving mechanic by id: {}", id);
        return new MechanicDTO(id, null);
    }
}