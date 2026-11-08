package br.com.officyna.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.assertj.core.api.Assertions.assertThat;

class AwsSqsConfigTest {

    @Test
    @DisplayName("Deve instanciar SqsClient com endpoint e credenciais")
    void sqsClient_ShouldInstantiateWithEndpointAndCredentials() {
        AwsSqsConfig config = new AwsSqsConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "awsEndpoint", "http://localhost:4566");
        ReflectionTestUtils.setField(config, "awsAccessKey", "test");
        ReflectionTestUtils.setField(config, "awsSecretKey", "test");

        try (SqsClient client = config.sqsClient()) {
            assertThat(client).isNotNull();
            assertThat(client.serviceName()).isEqualTo("sqs");
        }
    }

    @Test
    @DisplayName("Deve instanciar SqsClient sem endpoint customizado")
    void sqsClient_ShouldInstantiateWithoutEndpointOverride() {
        AwsSqsConfig config = new AwsSqsConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "awsEndpoint", "");
        ReflectionTestUtils.setField(config, "awsAccessKey", "");
        ReflectionTestUtils.setField(config, "awsSecretKey", "");

        try (SqsClient client = config.sqsClient()) {
            assertThat(client).isNotNull();
        }
    }
}
