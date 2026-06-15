package com.gateway.agent.provider;

import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.routing.RoutingDecision;
import reactor.core.publisher.Mono;

/**
 * Boundary for upstream model providers. New provider integrations should implement this interface
 * rather than adding provider-specific logic to controllers or gateway services.
 */
public interface LlmProviderClient {

    /**
     * Returns true when this client can handle the configured provider key.
     */
    boolean supports(String provider);

    /**
     * Creates a chat completion using the request and route selected by {@link com.gateway.agent.routing.RoutingStrategy}.
     */
    Mono<ChatCompletionResponse> createChatCompletion(ChatCompletionRequest request, RoutingDecision routingDecision);
}
