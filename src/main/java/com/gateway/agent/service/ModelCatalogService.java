package com.gateway.agent.service;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ModelListResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelCatalogService {

    private final GatewayProperties gatewayProperties;

    public ModelCatalogService(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public ModelListResponse listModels() {
        ModelListResponse response = new ModelListResponse();
        List<ModelListResponse.Model> models = gatewayProperties.getModelCatalog().stream()
                .map(definition -> {
                    ModelListResponse.Model model = new ModelListResponse.Model();
                    model.setId(definition.getId());
                    model.setObject(definition.getObject());
                    model.setOwnedBy(definition.getOwnedBy());
                    model.setCreated(System.currentTimeMillis() / 1000);
                    return model;
                })
                .toList();
        response.setData(models);
        return response;
    }
}
