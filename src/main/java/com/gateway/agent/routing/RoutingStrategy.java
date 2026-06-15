package com.gateway.agent.routing;

import com.gateway.agent.config.GatewayProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 模型路由策略。
 *
 * <p>客户端只关心 model 名称，例如 gateway-stub-chat；网关内部需要把这个 model 映射到
 * 具体的 route/provider/channel。这个类就是当前版本的路由查找入口。</p>
 */
@Component
public class RoutingStrategy {

    private final GatewayProperties gatewayProperties;

    public RoutingStrategy(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * 从配置里的 routes 列表中，查找第一个声明支持该 model 的路由。
     *
     * <p>当前是最简单的“顺序匹配”策略。后续如果要做权重、健康检查、失败切换，
     * 可以在这里替换成更复杂的策略，而不需要改 Controller。</p>
     */
    public Optional<RoutingDecision> resolve(String modelId) {
        return gatewayProperties.getRoutes().stream()
                .filter(route -> route.getModels().contains(modelId))
                .findFirst()
                .map(route -> new RoutingDecision(route.getRouteId(), route.getProvider(), route.getChannel()));
    }
}
