package com.gateway.agent.security;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.error.GatewayException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyWebFilter implements WebFilter {

    private final GatewayProperties gatewayProperties;

    public ApiKeyWebFilter(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!gatewayProperties.getAuth().isEnabled() || isPublicPath(exchange)) {
            return chain.filter(exchange);
        }

        String configuredKey = gatewayProperties.getAuth().getApiKey();
        if (configuredKey == null || configuredKey.isBlank() || "change-me".equals(configuredKey)) {
            return writeError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "api_error", "auth_not_configured",
                    "Gateway API key is not configured");
        }

        String providedKey = extractApiKey(exchange);
        if (providedKey == null || providedKey.isBlank()) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "authentication_error", "missing_api_key",
                    "Missing gateway API key");
        }

        if (!constantTimeEquals(providedKey, configuredKey)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "authentication_error", "invalid_api_key",
                    "Invalid gateway API key");
        }

        return chain.filter(exchange);
    }

    private boolean isPublicPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return path.equals("/")
                || path.equals("/health")
                || path.equals("/actuator/health")
                || path.startsWith("/admin/")
                || path.equals("/admin");
    }

    private String extractApiKey(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return headers.getFirst(gatewayProperties.getAuth().getHeaderName());
    }

    private boolean constantTimeEquals(String providedKey, String configuredKey) {
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        byte[] configured = configuredKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, configured);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String type, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":{\"message\":\"" + escapeJson(message) + "\",\"type\":\"" + type
                + "\",\"code\":\"" + code + "\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
