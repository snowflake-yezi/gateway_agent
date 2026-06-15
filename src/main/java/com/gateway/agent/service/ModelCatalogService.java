package com.gateway.agent.service;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ModelListResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ModelCatalogService {

    private final GatewayProperties gatewayProperties;

    public ModelCatalogService(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public ModelListResponse listModels() {
        ModelListResponse response = new ModelListResponse();
        // /v1/models is served from configuration so the public model list stays deployment-driven.
        List<ModelListResponse.Model> models = gatewayProperties.getModelCatalog().stream()
                .map(definition -> {
                    ModelListResponse.Model model = new ModelListResponse.Model();
                    model.setId(definition.getId());
                    model.setObject(definition.getObject());
                    model.setOwnedBy(definition.getOwnedBy());
                    // The scaffold does not persist model metadata yet, so created is generated per response.
                    model.setCreated(System.currentTimeMillis() / 1000);
                    return model;
                })
                .collect(Collectors.toList());
        response.setData(models);
        return response;
    }
}
