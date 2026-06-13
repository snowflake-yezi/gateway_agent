package com.gateway.agent.api;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.security.ApiKeyWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(HealthController.class)
@Import(ApiKeyWebFilter.class)
class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void healthReturnsOkWithoutApiKey() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.service").isEqualTo("gateway-agent");
    }
}
