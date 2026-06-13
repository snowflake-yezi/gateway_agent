package com.gateway.agent.service;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.error.GatewayException;
import com.gateway.agent.provider.StubLlmProviderClient;
import com.gateway.agent.routing.RoutingStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCompletionServiceTest {

    @Test
    void createCompletionUsesStubProviderForConfiguredModel() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ModelDefinition model = new GatewayProperties.ModelDefinition();
        model.setId("gateway-stub-chat");
        model.setObject("model");
        model.setOwnedBy("gateway-agent");
        model.setRoute("stub-chat");
        properties.setModelCatalog(List.of(model));

        GatewayProperties.RouteDefinition route = new GatewayProperties.RouteDefinition();
        route.setRouteId("stub-chat");
        route.setProvider("stub");
        route.setChannel("chat");
        route.setDisplayName("Stub Chat Provider");
        route.setModels(List.of("gateway-stub-chat"));
        properties.setRoutes(List.of(route));

        RoutingStrategy routingStrategy = new RoutingStrategy(properties);
        ChatCompletionService service = new ChatCompletionService(routingStrategy, List.of(new StubLlmProviderClient()));

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("gateway-stub-chat");
        ChatCompletionRequest.Message message = new ChatCompletionRequest.Message();
        message.setRole("user");
        message.setContent("hello");
        request.setMessages(List.of(message));

        StepVerifier.create(service.createCompletion(request))
                .assertNext(response -> {
                    assertThat(response.getModel()).isEqualTo("gateway-stub-chat");
                    assertThat(response.getChoices()).hasSize(1);
                    assertThat(response.getChoices()[0].getMessage().getContent()).contains("stub-chat");
                })
                .verifyComplete();
    }

    @Test
    void createCompletionRejectsStreamingForNow() {
        ChatCompletionService service = new ChatCompletionService(new RoutingStrategy(new GatewayProperties()), List.of());
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("gateway-stub-chat");
        request.setStream(true);

        StepVerifier.create(service.createCompletion(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(GatewayException.class);
                    GatewayException gatewayException = (GatewayException) error;
                    assertThat(gatewayException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(gatewayException.getCode()).isEqualTo("stream_not_supported");
                })
                .verify();
    }
}
