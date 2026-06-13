# Project Context

This document records non-sensitive context for future sessions working on `gateway_agent`.

## Goal

Build a compliant, self-hosted OpenAI-compatible gateway that can be used by API clients and developer tools. The project should aggregate authorized provider/API-key based channels over time. It should not implement private web-account scraping, cookie/token extraction, account-pool bypass, CAPTCHA/anti-bot evasion, or other circumvention workflows.

## Current state

- Spring Boot 3 + WebFlux Maven service.
- Java 17 target.
- Docker and Docker Compose deployment files exist.
- Public endpoints:
  - `GET /health`
  - `GET /actuator/health`
  - static admin UI at `/admin/index.html`
- Protected gateway endpoints:
  - `GET /v1/models`
  - `POST /v1/chat/completions`
- Authentication is API-key based:
  - `Authorization: Bearer <GATEWAY_API_KEY>`
  - or `X-API-Key: <GATEWAY_API_KEY>` by default.
- Current chat provider is a deterministic stub provider; it does not call a real model yet.
- `stream=true` is intentionally rejected until SSE support is implemented.

## Deployment conventions

- Runtime settings are supplied through `.env`; commit only `.env.example`.
- `GATEWAY_API_KEY` must be changed from `change-me` before exposing the service.
- Docker Compose service name: `gateway-agent`.
- Container health checks should probe `/actuator/health` and remain unauthenticated.
- Keep secrets, server IPs, provider keys, account credentials, and private operational details out of repository docs.

## Important files

- `pom.xml` — Maven/Spring Boot build.
- `src/main/resources/application.yml` — gateway defaults, model catalog, route config, auth env bindings.
- `src/main/java/com/gateway/agent/security/ApiKeyWebFilter.java` — API key auth filter.
- `src/main/resources/static/admin/` — dependency-free admin UI.
- `docker-compose.yml` — server-friendly Compose deployment.
- `.env.example` — safe deployment defaults.
- `README.md` — human-facing setup and smoke tests.
- `CLAUDE.md` — guidance for future Claude Code sessions.

## Likely next milestones

1. Add SSE streaming support for `stream=true`.
2. Add a real authorized provider adapter.
3. Add user/API-key management and per-key quotas.
4. Add persistence for conversations or usage logs if needed.
5. Add reverse proxy/HTTPS deployment guidance.
