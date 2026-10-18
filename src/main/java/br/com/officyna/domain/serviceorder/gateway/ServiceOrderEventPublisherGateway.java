package br.com.officyna.domain.serviceorder.gateway;

public interface ServiceOrderEventPublisherGateway {

    void publishForDiagnostics(String serviceOrderId);
}
