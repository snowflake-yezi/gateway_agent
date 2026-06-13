# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`gateway_agent` is a Spring Boot 3 + WebFlux scaffold for an OpenAI-compatible gateway. The first milestone is a thin gateway surface with `/health`, `/v1/models`, and `/v1/chat/completions`, plus routing seams that can later expand into multi-channel/provider selection. See `docs/context.md` for non-sensitive project context and handoff notes.

## Common commands

- Run the app: `mvn spring-boot:run`
- Run all tests: `mvn test`
- Run a single test class: `mvn -Dtest=ChatCompletionServiceTest test`
- Run a single test method: `mvn -Dtest=ChatCompletionServiceTest#createCompletionUsesStubProviderForConfiguredModel test`
- Build Docker image: `docker build -t gateway-agent:local .`
- Run container: `docker run --rm -p 8080:8080 --name gateway-agent -e GATEWAY_API_KEY=sk-local-dev gateway-agent:local`
- Validate Compose config: `docker compose config`
- Run with Compose: `docker compose up -d --build`
- Check Compose status: `docker compose ps`
- Tail Compose logs: `docker compose logs -f gateway-agent`
- Stop Compose: `docker compose down`

## Architecture

- `src/main/java/com/gateway/agent/GatewayAgentApplication.java` is the Spring Boot entrypoint.
- `src/main/java/com/gateway/agent/api/` contains the HTTP surface:
  - `HealthController` for `/health`
  - `ModelsController` for `/v1/models`
  - `ChatCompletionsController` for `/v1/chat/completions`
- `src/main/java/com/gateway/agent/dto/openai/` holds OpenAI-shaped request/response payloads used by the public API.
- `src/main/java/com/gateway/agent/security/ApiKeyWebFilter.java` protects `/v1/**` with API key auth while leaving health and static admin resources public.
- `src/main/java/com/gateway/agent/service/` holds orchestration logic:
  - `ModelCatalogService` builds the model list from configuration.
  - `ChatCompletionService` resolves routing and delegates to a provider client.
- `src/main/java/com/gateway/agent/routing/` contains the routing seam. `RoutingStrategy` maps a model name to a configured route, and `RoutingDecision` carries the resolved provider/channel identifiers.
- `src/main/java/com/gateway/agent/provider/` is the upstream abstraction layer. `LlmProviderClient` is the contract; `StubLlmProviderClient` is the initial implementation.
- `src/main/java/com/gateway/agent/config/GatewayProperties.java` binds `gateway.*` config from `application.yml` and defines auth, model catalog, and route registry.
- `src/main/java/com/gateway/agent/error/GlobalExceptionHandler.java` converts validation and gateway errors into OpenAI-style error envelopes.
- `src/main/resources/static/admin/` contains the dependency-free browser admin/testing UI.
- `src/main/resources/application.yml` is the central configuration file for server settings, actuator exposure, auth env bindings, model catalog entries, and route definitions.
- `Dockerfile` uses a multi-stage Maven build and a Java 17 runtime image.
- `docker-compose.yml` is the preferred deployment entry point for simple server installs and uses `/actuator/health` for container health checks.

## Deployment conventions

- Copy `.env.example` to `.env` before using Compose.
- Change `GATEWAY_API_KEY` from `change-me` before exposing the service.
- Protected endpoints accept `Authorization: Bearer <key>` and the configured `X-API-Key` style header.
- Compose uses:
  - `HOST_PORT` for the published host port
  - `SERVER_PORT` for the Spring Boot port inside the container
  - `SPRING_PROFILES_ACTIVE` for the Spring profile
  - `GATEWAY_API_KEY` for gateway client auth
- `docker-compose.yml` should keep `restart: unless-stopped` for server-friendly behavior.
- `.gitignore` should keep `target/`, `.env`, IDE files, and local tooling directories out of commits.
- `.env.example` is safe to commit; real `.env` files are not.
- Container health checks should probe `/actuator/health` rather than `/health` and must remain unauthenticated.

## Current behavior

- `GET /v1/models` is config-backed and returns OpenAI-style `data` entries.
- `POST /v1/chat/completions` accepts an OpenAI-style request and returns a deterministic stub response through the configured provider seam.
- `stream=true` is intentionally rejected for now with a structured error; streaming support is planned for a later step.
- Routing is configuration-driven rather than hardcoded in controllers.
- Admin UI is available at `/admin/index.html`.

## Testing and deployment notes

- WebFlux endpoint tests live under `src/test/java/com/gateway/agent/api/`.
- Service-level routing and response-shaping tests live under `src/test/java/com/gateway/agent/service/`.
- The Docker build context is trimmed by `.dockerignore`; keep `pom.xml` and `src/` available for image builds.
- Do not add docs or implementation paths for private web-account scraping, cookie extraction, anti-bot bypass, or account-pool circumvention. Keep the project on authorized provider/API-key based integrations.
