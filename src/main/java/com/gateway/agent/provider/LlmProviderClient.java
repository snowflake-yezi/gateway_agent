package com.gateway.agent.provider;

import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.routing.RoutingDecision;
import reactor.core.publisher.Mono;

public interface LlmProviderClient {

    boolean supports(String provider);

    Mono<ChatCompletionResponse> createChatCompletion(ChatCompletionRequest request, RoutingDecision routingDecision);
}
