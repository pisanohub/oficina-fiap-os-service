package br.com.fiap.soat15.osservice.config.observability;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(
        ObservabilityEndpointsIntegrationTest.TestConfig.class
)
class ObservabilityEndpointsIntegrationTest {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void deveCriarEndpointsDeHealthEMetricasPrometheus() {

        assertThat(healthEndpoint).isNotNull();
        assertThat(prometheusMeterRegistry).isNotNull();

        prometheusMeterRegistry
                .counter("oficina.observabilidade.teste")
                .increment();

        assertThat(prometheusMeterRegistry.scrape())
                .contains("oficina_observabilidade_teste_total");
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            SqsAutoConfiguration.class
    })
    static class TestConfig {
    }
}