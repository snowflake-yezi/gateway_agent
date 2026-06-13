package com.gateway.agent.api;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ModelListResponse;
import com.gateway.agent.security.ApiKeyWebFilter;
import com.gateway.agent.service.ModelCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

@WebFluxTest(ModelsController.class)
@Import(ApiKeyWebFilter.class)
class ModelsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GatewayProperties gatewayProperties;

    @MockBean
    private ModelCatalogService modelCatalogService;

    @Test
    void listModelsRejectsMissingApiKey() {
        gatewayProperties.getAuth().setApiKey("test-key");

        webTestClient.get()
                .uri("/v1/models")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("missing_api_key");
    }

    @Test
    void listModelsReturnsOpenAiStyleList() {
        gatewayProperties.getAuth().setApiKey("test-key");
        ModelListResponse response = new ModelListResponse();
        ModelListResponse.Model model = new ModelListResponse.Model();
        model.setId("gateway-stub-chat");
        model.setObject("model");
        model.setCreated(1L);
        model.setOwnedBy("gateway-agent");
        response.setData(java.util.List.of(model));

        when(modelCatalogService.listModels()).thenReturn(response);

        webTestClient.get()
                .uri("/v1/models")
                .header("Authorization", "Bearer test-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.object").isEqualTo("list")
                .jsonPath("$.data[0].id").isEqualTo("gateway-stub-chat")
                .jsonPath("$.data[0].owned_by").isEqualTo("gateway-agent");
    }
}
