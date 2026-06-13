package com.gateway.agent.api;

import com.gateway.agent.GatewayAgentApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = GatewayAgentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminStaticResourceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void adminIndexIsPublicStaticResource() {
        webTestClient.get()
                .uri("/admin/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body).contains("Gateway Admin"));
    }
}
