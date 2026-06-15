package com.gateway.agent.provider;

import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.routing.RoutingDecision;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Deterministic scaffold provider used to verify request, routing, and response plumbing before real upstreams exist.
 */
@Component
public class StubLlmProviderClient implements LlmProviderClient {

    @Override
    public boolean supports(String provider) {
        return "stub".equalsIgnoreCase(provider);
    }

    @Override
    public Mono<ChatCompletionResponse> createChatCompletion(ChatCompletionRequest request, RoutingDecision routingDecision) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-" + UUID.randomUUID());
        response.setObject("chat.completion");
        response.setCreated(Instant.now().getEpochSecond());
        response.setModel(request.getModel());

        ChatCompletionResponse.Message message = new ChatCompletionResponse.Message();
        message.setRole("assistant");
        // Include the route id so smoke tests make the selected route visible.
        message.setContent("Stub response from route " + routingDecision.routeId());

        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason("stop");
        response.setChoices(new ChatCompletionResponse.Choice[] {choice});

        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(0);
        usage.setCompletionTokens(0);
        usage.setTotalTokens(0);
        response.setUsage(usage);

        return Mono.just(response);
    }
}
