package com.gateway.agent.service;

import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.error.GatewayException;
import com.gateway.agent.provider.LlmProviderClient;
import com.gateway.agent.routing.RoutingDecision;
import com.gateway.agent.routing.RoutingStrategy;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChatCompletionService {

    private final RoutingStrategy routingStrategy;
    private final List<LlmProviderClient> providerClients;

    public ChatCompletionService(RoutingStrategy routingStrategy, List<LlmProviderClient> providerClients) {
        this.routingStrategy = routingStrategy;
        this.providerClients = providerClients;
    }

    public Mono<ChatCompletionResponse> createCompletion(ChatCompletionRequest request) {
        if (Boolean.TRUE.equals(request.getStream())) {
            return Mono.error(new GatewayException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_request_error",
                    "stream_not_supported",
                    "Streaming is not supported in the initial scaffold"
            ));
        }

        RoutingDecision routingDecision = routingStrategy.resolve(request.getModel())
                .orElseThrow(() -> new GatewayException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_request_error",
                        "model_not_found",
                        "Model '%s' is not configured".formatted(request.getModel())
                ));

        LlmProviderClient providerClient = providerClients.stream()
                .filter(client -> client.supports(routingDecision.provider()))
                .findFirst()
                .orElseThrow(() -> new GatewayException(
                        HttpStatus.BAD_GATEWAY,
                        "api_error",
                        "provider_not_available",
                        "No provider client is available for '%s'".formatted(routingDecision.provider())
                ));

        return providerClient.createChatCompletion(request, routingDecision);
    }
}
