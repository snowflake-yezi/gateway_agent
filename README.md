# gateway_agent

A Spring Boot 3 + WebFlux scaffold for an OpenAI-compatible gateway.

## Run locally

Set an API key before calling protected `/v1` endpoints:

```bash
set GATEWAY_API_KEY=sk-local-dev
mvn spring-boot:run
```

PowerShell:

```powershell
$env:GATEWAY_API_KEY = "sk-local-dev"
mvn spring-boot:run
```

## Run tests

- `mvn test`
- Single test: `mvn -Dtest=ChatCompletionServiceTest test`
- Single test method: `mvn -Dtest=ChatCompletionServiceTest#createCompletionUsesStubProviderForConfiguredModel test`

## Admin UI

Open the lightweight admin/testing page after the app starts:

- `http://localhost:8080/admin/index.html`

The page can check health endpoints, load models, and send a chat test request. Enter `GATEWAY_API_KEY` in the API Key field before calling `/v1` endpoints.

## Docker

### Build image

- `docker build -t gateway-agent:local .`

### Run container directly

```bash
docker run --rm -p 8080:8080 --name gateway-agent -e GATEWAY_API_KEY=sk-local-dev gateway-agent:local
```

### Run with Docker Compose

Create a runtime env file first:

```bash
cp .env.example .env
```

Edit `.env` and change `GATEWAY_API_KEY` before exposing the service.

Then start the service:

```bash
docker compose up -d --build
```

Useful commands:

```bash
docker compose ps
docker compose logs -f gateway-agent
docker compose restart gateway-agent
docker compose down
```

`docker-compose.yml` uses these defaults from `.env.example`:

```env
HOST_PORT=8080
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=default
GATEWAY_AUTH_ENABLED=true
GATEWAY_API_KEY=change-me
GATEWAY_API_KEY_HEADER=X-API-Key
```

The container health check probes `/actuator/health`.

## Available endpoints

- `GET /health` — public
- `GET /actuator/health` — public
- `GET /admin/index.html` — public static admin UI
- `GET /v1/models` — protected
- `POST /v1/chat/completions` — protected

## Current behavior

- `GET /v1/models` serves a config-backed OpenAI-style model list.
- `POST /v1/chat/completions` accepts an OpenAI-style request and returns a deterministic stub response.
- Routing is configuration-driven through `gateway.model-catalog` and `gateway.routes` in `src/main/resources/application.yml`.
- Streaming is intentionally not wired in the initial scaffold and returns a structured 400 error.

## Smoke tests

### Windows CMD

- `curl http://localhost:8080/health`
- `curl http://localhost:8080/actuator/health`
- `curl -H "Authorization: Bearer sk-local-dev" http://localhost:8080/v1/models`
- `curl -X POST http://localhost:8080/v1/chat/completions -H "Authorization: Bearer sk-local-dev" -H "Content-Type: application/json" -d "{\"model\":\"gateway-stub-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}"`

### PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/v1/models" -Headers @{ Authorization = "Bearer sk-local-dev" } -Method Get
$body = @{ model = "gateway-stub-chat"; messages = @(@{ role = "user"; content = "hello" }) } | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri "http://localhost:8080/v1/chat/completions" -Headers @{ Authorization = "Bearer sk-local-dev" } -Method Post -ContentType "application/json" -Body $body
```

### Linux/macOS shell

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
curl -H "Authorization: Bearer sk-local-dev" http://localhost:8080/v1/models
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-local-dev" \
  -H "Content-Type: application/json" \
  -d '{"model":"gateway-stub-chat","messages":[{"role":"user","content":"hello"}]}'
```

## Context handoff

See `docs/context.md` for non-sensitive project context and future-session handoff notes.
