package br.com.officyna.api.customer.controller;


import br.com.officyna.api.customer.CustomerApi;
import br.com.officyna.api.customer.resources.CustomerRequest;
import br.com.officyna.api.customer.resources.CustomerResponse;
import br.com.officyna.domain.customer.controller.CustomerControllerAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerControllerAdapter customerControllerAdapter;

    @Override
    public ResponseEntity<List<CustomerResponse>> findAll() {
        return ResponseEntity.ok(customerControllerAdapter.findAll());
    }

    @Override
    public ResponseEntity<CustomerResponse> findById(String id) {
        return ResponseEntity.ok(customerControllerAdapter.findById(id));
    }

    @Override
    public ResponseEntity<CustomerResponse> findByDocument(String document) {

        CustomerResponse response = customerControllerAdapter.findByDocument(document);

        logger.info("Customer found by document");

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CustomerResponse> create(CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerControllerAdapter.create(request));
    }

    @Override
    public ResponseEntity<CustomerResponse> update(String id, CustomerRequest request) {
        return ResponseEntity.ok(customerControllerAdapter.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        customerControllerAdapter.delete(id);
        return ResponseEntity.noContent().build();
    }
}