package com.gateway.agent.api;

import com.gateway.agent.dto.openai.ModelListResponse;
import com.gateway.agent.service.ModelCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/models")
public class ModelsController {

    private final ModelCatalogService modelCatalogService;

    public ModelsController(ModelCatalogService modelCatalogService) {
        this.modelCatalogService = modelCatalogService;
    }

    @GetMapping
    public Mono<ModelListResponse> listModels() {
        return Mono.fromSupplier(modelCatalogService::listModels);
    }
}
