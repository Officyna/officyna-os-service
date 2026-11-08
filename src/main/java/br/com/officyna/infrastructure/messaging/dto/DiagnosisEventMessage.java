package br.com.officyna.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisEventMessage {

    private String serviceOrderId;

    @JsonAlias({"mechanichId", "mechanic_id"})
    private String mechanicId;

    @JsonAlias({"mechanic_name"})
    private String mechanicName;
}
