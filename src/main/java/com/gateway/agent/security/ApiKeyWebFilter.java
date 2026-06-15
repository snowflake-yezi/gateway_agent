package com.gateway.agent.security;

import com.gateway.agent.config.GatewayProperties;
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

/**
 * 网关入口鉴权过滤器。
 *
 * <p>这里没有引入 Spring Security，而是用 WebFlux 的 {@link WebFilter} 做一个轻量级 API Key 校验。
 * 这样当前阶段依赖更少，也更容易看清请求进入 Controller 之前发生了什么。</p>
 *
 * <p>保护范围是 /v1/** 这类真正会消耗模型资源的接口；健康检查和静态管理页面保持公开，
 * 方便 Docker 健康检查和浏览器打开管理页。管理页本身不会绕过鉴权，它调用 /v1 接口时仍然会带上 API Key。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyWebFilter implements WebFilter {

    private final GatewayProperties gatewayProperties;

    public ApiKeyWebFilter(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 如果配置里关闭了鉴权，或者当前路径是公开路径，就直接放行到后续过滤器/Controller。
        if (!gatewayProperties.getAuth().isEnabled() || isPublicPath(exchange)) {
            return chain.filter(exchange);
        }

        String configuredKey = gatewayProperties.getAuth().getApiKey();
        // change-me 是仓库里的示例默认值；如果线上仍然使用它，说明部署时忘记配置真实密钥。
        // 这里返回 500 而不是放行，是为了避免服务以默认弱密钥暴露到公网。
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
        // 这些路径故意不鉴权：
        // 1. /health 和 /actuator/health 供 Docker/云服务器健康检查使用。
        // 2. /admin/** 只是静态页面资源，页面真正调用 /v1 接口时仍然需要 API Key。
        // 3. / 保留为公开入口，后续如果要做首页跳转可以直接复用。
        return path.equals("/")
                || path.equals("/health")
                || path.equals("/actuator/health")
                || path.startsWith("/admin/")
                || path.equals("/admin");
    }

    private String extractApiKey(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        // 优先支持 OpenAI 兼容客户端常用的 Authorization: Bearer <key>。
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        // 同时保留 X-API-Key 这类自定义 Header，方便 curl、内部服务或后续管理后台调用。
        return headers.getFirst(gatewayProperties.getAuth().getHeaderName());
    }

    private boolean constantTimeEquals(String providedKey, String configuredKey) {
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        byte[] configured = configuredKey.getBytes(StandardCharsets.UTF_8);
        // 使用 JDK 提供的常量时间比较，避免用 String.equals 暴露不必要的时序差异。
        return MessageDigest.isEqual(provided, configured);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String type, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 这是过滤器层的失败，未必会进入 @RestControllerAdvice，所以在这里直接写出 OpenAI 风格错误结构。
        String body = "{\"error\":{\"message\":\"" + escapeJson(message) + "\",\"type\":\"" + type
                + "\",\"code\":\"" + code + "\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
