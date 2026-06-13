package com.gateway.agent.routing;

import com.gateway.agent.config.GatewayProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RoutingStrategy {

    private final GatewayProperties gatewayProperties;

    public RoutingStrategy(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public Optional<RoutingDecision> resolve(String modelId) {
        return gatewayProperties.getRoutes().stream()
                .filter(route -> route.getModels().contains(modelId))
                .findFirst()
                .map(route -> new RoutingDecision(route.getRouteId(), route.getProvider(), route.getChannel()));
    }
}
