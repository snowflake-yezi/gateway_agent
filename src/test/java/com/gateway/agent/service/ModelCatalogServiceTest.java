package com.gateway.agent.service;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ModelListResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCatalogServiceTest {

    @Test
    void listModelsBuildsOpenAiStyleResponse() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ModelDefinition definition = new GatewayProperties.ModelDefinition();
        definition.setId("gateway-stub-chat");
        definition.setObject("model");
        definition.setOwnedBy("gateway-agent");
        definition.setRoute("stub-chat");
        properties.setModelCatalog(List.of(definition));
        properties.setRoutes(List.of());

        ModelCatalogService service = new ModelCatalogService(properties);
        ModelListResponse response = service.listModels();

        assertThat(response.getObject()).isEqualTo("list");
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo("gateway-stub-chat");
        assertThat(response.getData().get(0).getOwnedBy()).isEqualTo("gateway-agent");
    }
}
