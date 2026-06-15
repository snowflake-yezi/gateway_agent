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

/**
 * Chat Completions 主流程编排层。
 *
 * <p>Controller 只负责 HTTP 入参/出参，真正的网关逻辑放在这里：先检查当前请求是否支持，
 * 再根据 model 找路由，最后把请求交给对应 ProviderClient。这样以后增加真实上游供应商时，
 * 不需要改 Controller，只需要新增 ProviderClient 和配置。</p>
 */
@Service
public class ChatCompletionService {

    private final RoutingStrategy routingStrategy;
    private final List<LlmProviderClient> providerClients;

    public ChatCompletionService(RoutingStrategy routingStrategy, List<LlmProviderClient> providerClients) {
        this.routingStrategy = routingStrategy;
        this.providerClients = providerClients;
    }

    public Mono<ChatCompletionResponse> createCompletion(ChatCompletionRequest request) {
        // 当前版本只实现了非流式 JSON 返回。stream=true 需要 SSE 分块响应，后续单独实现。
        if (Boolean.TRUE.equals(request.getStream())) {
            return Mono.error(new GatewayException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_request_error",
                    "stream_not_supported",
                    "Streaming is not supported in the initial scaffold"
            ));
        }

        // 第一步：把用户传入的 model 映射到配置里的 route。
        // 这样客户端可以一直使用稳定的模型别名，后端可以通过配置把它切到不同 provider。
        RoutingDecision routingDecision = routingStrategy.resolve(request.getModel())
                .orElseThrow(() -> new GatewayException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_request_error",
                        "model_not_found",
                        "Model '%s' is not configured".formatted(request.getModel())
                ));

        // 第二步：根据 route.provider 找到能处理该 provider 的客户端实现。
        // 所有 LlmProviderClient 都是 Spring Bean，新增 OpenAI/Anthropic/OpenRouter 等实现后会自动注入到列表里。
        LlmProviderClient providerClient = providerClients.stream()
                .filter(client -> client.supports(routingDecision.provider()))
                .findFirst()
                .orElseThrow(() -> new GatewayException(
                        HttpStatus.BAD_GATEWAY,
                        "api_error",
                        "provider_not_available",
                        "No provider client is available for '%s'".formatted(routingDecision.provider())
                ));

        // 第三步：把标准 OpenAI 风格请求交给具体 provider 实现。
        return providerClient.createChatCompletion(request, routingDecision);
    }
}
