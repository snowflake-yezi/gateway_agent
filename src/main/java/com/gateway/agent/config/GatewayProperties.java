package com.gateway.agent.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    @NotNull
    private Auth auth = new Auth();

    @NotNull
    private List<ModelDefinition> modelCatalog = new ArrayList<>();

    @NotNull
    private List<RouteDefinition> routes = new ArrayList<>();

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public List<ModelDefinition> getModelCatalog() {
        return modelCatalog;
    }

    public void setModelCatalog(List<ModelDefinition> modelCatalog) {
        this.modelCatalog = modelCatalog;
    }

    public List<RouteDefinition> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDefinition> routes) {
        this.routes = routes;
    }

    public static class Auth {
        private boolean enabled = true;
        private String apiKey = "change-me";
        private String headerName = "X-API-Key";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    public static class ModelDefinition {
        @NotBlank
        private String id;
        @NotBlank
        private String object;
        @NotBlank
        private String ownedBy;
        @NotBlank
        private String route;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public String getOwnedBy() {
            return ownedBy;
        }

        public void setOwnedBy(String ownedBy) {
            this.ownedBy = ownedBy;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }
    }

    public static class RouteDefinition {
        @NotBlank
        private String routeId;
        @NotBlank
        private String provider;
        @NotBlank
        private String channel;
        @NotBlank
        private String displayName;
        @NotEmpty
        private List<String> models = new ArrayList<>();

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getModels() {
            return models;
        }

        public void setModels(List<String> models) {
            this.models = models;
        }
    }
}
