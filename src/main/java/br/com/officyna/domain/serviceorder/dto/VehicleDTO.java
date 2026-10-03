package br.com.officyna.domain.serviceorder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VehicleDTO {

    private String id;

    private String plate;

    private String brand;

    private String model;

    private String color;
}
