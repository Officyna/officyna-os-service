package br.com.officyna.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiagnosisEventMessage {

    private String serviceOrderId;

    @JsonAlias({"mechanichId", "mechanic_id"})
    private String mechanicId;

    @JsonAlias({"mechanic_name"})
    private String mechanicName;
}
