package br.com.officyna.api.serviceorder.controller;

import br.com.officyna.api.serviceorder.CustomerServiceOrderApi;
import br.com.officyna.api.serviceorder.resources.ModifySituationRequest;
import br.com.officyna.api.serviceorder.resources.ServiceOrderResponse;
import br.com.officyna.domain.serviceorder.controller.CustomerServiceOrderControllerAdapter;
import br.com.officyna.domain.serviceorder.enums.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerServiceOrderController implements CustomerServiceOrderApi {

    private final CustomerServiceOrderControllerAdapter customerServiceOrderControllerAdapter;

    @Override
    public ResponseEntity<List<ServiceOrderResponse>> findByCustomerDocument(String document, ServiceOrderStatus status) {
        return ResponseEntity.ok(customerServiceOrderControllerAdapter.findByCustomerDocument(document, status));
    }

    @Override
    public ResponseEntity<ServiceOrderResponse> aprovalLabors(String id, List<ModifySituationRequest> request) {
       return ResponseEntity.ok(customerServiceOrderControllerAdapter.updateLaborSituation(id, request));
    }
}