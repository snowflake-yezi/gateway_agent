package com.gateway.agent.routing;

/**
 * 路由决策结果。
 *
 * <p>RoutingStrategy 根据请求里的 model 找到 route 后，会把 routeId/provider/channel 打包成这个对象，
 * 继续传给 ChatCompletionService 和 LlmProviderClient。它本身不包含 HTTP 或上游请求细节，
 * 只是一个很轻量的“路由选择结果”。</p>
 *
 * @param routeId 配置里的路由 ID，例如 stub-chat
 * @param provider provider 标识，用来选择哪个 LlmProviderClient 处理请求
 * @param channel 逻辑能力类型，例如 chat；后续可扩展到 embeddings/images 等
 */
public record RoutingDecision(String routeId, String provider, String channel) {
}
