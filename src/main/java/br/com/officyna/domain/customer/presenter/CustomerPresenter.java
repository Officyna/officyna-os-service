package br.com.officyna.domain.customer.presenter;

import br.com.officyna.api.customer.resources.AddressDTO;
import br.com.officyna.api.customer.resources.CustomerResponse;
import br.com.officyna.domain.customer.entity.Address;
import br.com.officyna.domain.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerPresenter {

    public CustomerResponse toResponse(Customer entity) {
        return new CustomerResponse(
                entity.getId(),
                entity.getName(),
                entity.getDocument(),
                entity.getType(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAreaCode(),
                entity.getCountryCode(),
                toAddressRecord(entity.getAddress()),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }

    private AddressDTO toAddressRecord(Address entity) {
        if (entity == null) return null;
        return new AddressDTO(
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState(),
                entity.getZipCode(),
                entity.getCountry()
        );
    }
}