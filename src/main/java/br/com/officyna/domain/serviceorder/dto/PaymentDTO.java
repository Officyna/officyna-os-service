package br.com.officyna.domain.serviceorder.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {

    private String paymentLink;

    private String qrCode;

    private String qrCodeBase64;
}

