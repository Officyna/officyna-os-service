package br.com.officyna.infrastructure.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventForDiagnosticsMessage {

    private String serviceOrderId;

    @Builder.Default
    private String status = "RECEBIDA";
}
