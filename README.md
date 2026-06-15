# gateway_agent

这是一个基于 **Spring Boot 3 + WebFlux** 的 OpenAI 兼容网关脚手架项目。

它当前的目标不是直接接入真实模型平台，而是先把一套完整的网关骨架搭起来：

- 统一的 OpenAI 风格接口入口
- 基础鉴权
- 模型到路由的映射
- Provider 抽象层
- 一个可直接在浏览器里操作的轻量管理页面
- Docker / Docker Compose 部署方式

当前版本已经能跑通完整链路，但模型响应仍然是 **stub（桩实现）**，也就是模拟返回，不会真正请求外部模型。

---

## 一、项目现在能做什么

### 1. 已实现接口

公开接口：

- `GET /health`
- `GET /actuator/health`
- `GET /admin/index.html`

需要鉴权的接口：

- `GET /v1/models`
- `POST /v1/chat/completions`

### 2. 当前行为

- `/v1/models` 会返回配置文件里定义的模型列表。
- `/v1/chat/completions` 会接收 OpenAI 风格的请求体，并返回一个固定的 stub 响应。
- `stream=true` 目前还不支持，会返回结构化 400 错误。
- 管理页 `/admin/index.html` 可以直接在浏览器里测试健康检查、模型列表和 chat 请求。

### 3. 当前还没做的核心功能

- 真实 Provider 接入（例如 OpenAI-compatible 上游）
- SSE 流式响应
- 多渠道/多 Provider 管理
- 调用日志、配额、统计面板

---

## 二、项目结构说明

### 1. 后端入口

`src/main/java/com/gateway/agent/GatewayAgentApplication.java`

Spring Boot 启动类，整个服务从这里启动。

### 2. API 层

`src/main/java/com/gateway/agent/api/`

这里放对外暴露的 HTTP 接口：

- `HealthController`：`/health`
- `ModelsController`：`/v1/models`
- `ChatCompletionsController`：`/v1/chat/completions`

这一层尽量保持“薄”，主要负责：

- 接收请求
- 触发参数校验
- 调用 service
- 返回结果

真正的业务编排不放在 Controller 里。

### 3. Service 层

`src/main/java/com/gateway/agent/service/`

核心编排逻辑在这里：

- `ModelCatalogService`
  - 把配置里的模型定义转换成 `/v1/models` 的返回结果。
- `ChatCompletionService`
  - 负责 chat 请求的主流程：
    1. 检查当前请求是否支持
    2. 根据 model 查找 route
    3. 根据 route 选择 provider client
    4. 把请求交给 provider 执行

### 4. Routing 层

`src/main/java/com/gateway/agent/routing/`

这是“模型名 -> 路由 -> provider”的核心过渡层。

- `RoutingStrategy`
  - 根据传入的模型名，从配置里找到匹配的 route。
- `RoutingDecision`
  - 一个很轻量的不可变对象，用来把选出来的 routeId/provider/channel 传给 provider 层。

这层的意义是：

> 让客户端看到的是稳定的模型别名，而不是直接绑死某个真实上游。

以后你想把某个模型从一个 provider 切到另一个 provider，只要改配置，不用改 Controller。

### 5. Provider 层

`src/main/java/com/gateway/agent/provider/`

这里是“真正和上游模型平台打交道”的抽象层。

- `LlmProviderClient`
  - 所有上游实现都要遵守的接口。
- `StubLlmProviderClient`
  - 当前的桩实现，用来验证整个请求链路能否跑通。

也就是说，现在虽然接口看起来像真的，但实际上还没有调用真实模型平台。

### 6. 鉴权层

`src/main/java/com/gateway/agent/security/ApiKeyWebFilter.java`

这是一个 WebFlux 过滤器，位于 Controller 之前。

作用是：

- 判断哪些路径是公开的
- 判断哪些路径必须带 API Key
- 支持两种鉴权方式：
  - `Authorization: Bearer <key>`
  - `X-API-Key: <key>`
- 如果缺少 key、key 错误、或者服务端没配置 key，就直接返回 JSON 错误

这里没有引入 Spring Security，而是自己写了一个轻量过滤器，方便当前阶段更清晰地理解整个请求流程。

### 7. 配置层

`src/main/java/com/gateway/agent/config/GatewayProperties.java`

这个类负责把 `application.yml` 中的 `gateway.*` 配置绑定成 Java 对象。

它目前主要管理三部分：

- `auth`
  - 是否开启鉴权
  - API Key
  - 自定义 Header 名称
- `modelCatalog`
  - `/v1/models` 返回哪些模型
- `routes`
  - 模型该走哪个 provider、哪个 channel

---

## 三、配置文件说明

### `src/main/resources/application.yml`

这是服务的核心配置文件。

目前包含：

### 1. 服务端口

```yaml
server:
  port: 8080
```

默认监听 `8080`。

### 2. 健康检查

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

这表示会暴露 `/actuator/health`，给 Docker 或云服务器做健康探针使用。

### 3. 鉴权配置

```yaml
gateway:
  auth:
    enabled: ${GATEWAY_AUTH_ENABLED:true}
    api-key: ${GATEWAY_API_KEY:change-me}
    header-name: ${GATEWAY_API_KEY_HEADER:X-API-Key}
```

注意这里的默认值 `change-me` 只是示例值。

代码里明确把它视为“未正确配置”，也就是说：

> 如果你不改这个值，受保护接口会直接报 `auth_not_configured`。

这是故意设计的，用来防止你忘记配置密钥就把服务暴露出去。

### 4. 模型目录

```yaml
model-catalog:
```

它决定 `/v1/models` 对外返回哪些模型。

### 5. 路由配置

```yaml
routes:
```

它决定某个模型最终走哪个 provider。

当前只有一个：

- `gateway-stub-chat` -> `stub`

---

## 四、如何本地运行

### 1. 先设置 API Key

#### CMD

```bat
set GATEWAY_API_KEY=sk-local-dev
mvn spring-boot:run
```

#### PowerShell

```powershell
$env:GATEWAY_API_KEY = "sk-local-dev"
mvn spring-boot:run
```

> 注意：必须在**启动服务的同一个终端窗口**里设置环境变量。

### 2. 打开管理页面

```text
http://localhost:8080/admin/index.html
```

页面里输入：

```text
sk-local-dev
```

然后可以：

- 检查 `/health`
- 检查 `/actuator/health`
- 加载模型
- 发送 chat 请求

---

## 五、如何测试接口

### 1. 健康检查（无需鉴权）

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

### 2. 获取模型（需要鉴权）

```bash
curl -H "Authorization: Bearer sk-local-dev" http://localhost:8080/v1/models
```

### 3. 测试 chat completions（需要鉴权）

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-local-dev" \
  -H "Content-Type: application/json" \
  -d '{"model":"gateway-stub-chat","messages":[{"role":"user","content":"hello"}]}'
```

Windows CMD 单行：

```bat
curl -X POST http://localhost:8080/v1/chat/completions -H "Authorization: Bearer sk-local-dev" -H "Content-Type: application/json" -d "{\"model\":\"gateway-stub-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}"
```

---

## 六、Docker 部署说明

### 1. 直接构建镜像

```bash
docker build -t gateway-agent:local .
```

### 2. 直接运行容器

```bash
docker run --rm -p 8080:8080 --name gateway-agent -e GATEWAY_API_KEY=sk-local-dev gateway-agent:local
```

### 3. 使用 Docker Compose

先复制环境文件：

```bash
cp .env.example .env
```

然后编辑 `.env`，至少修改：

```env
GATEWAY_API_KEY=你的真实密钥
```

启动：

```bash
docker compose up -d --build
```

查看：

```bash
docker compose ps
docker compose logs -f gateway-agent
```

停止：

```bash
docker compose down
```

### 4. `.env.example` 字段说明

```env
HOST_PORT=8080
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=default
GATEWAY_AUTH_ENABLED=true
GATEWAY_API_KEY=change-me
GATEWAY_API_KEY_HEADER=X-API-Key
```

含义：

- `HOST_PORT`
  - 宿主机暴露端口
- `SERVER_PORT`
  - 容器内 Spring Boot 监听端口
- `SPRING_PROFILES_ACTIVE`
  - Spring Profile
- `GATEWAY_AUTH_ENABLED`
  - 是否启用鉴权
- `GATEWAY_API_KEY`
  - 网关 API Key
- `GATEWAY_API_KEY_HEADER`
  - 除 Bearer 外的备用 Header 名称

---

## 七、管理页面是怎么工作的

页面代码在：

```text
src/main/resources/static/admin/
```

主要文件：

- `index.html`
- `styles.css`
- `app.js`

### 页面逻辑

- API Key 会保存在浏览器 `localStorage`
- health 接口不用带鉴权
- `/v1/models` 和 `/v1/chat/completions` 会自动带上 Bearer Token
- 页面会显示：
  - 原始 JSON
  - assistant 返回内容

这个页面不是管理后台系统，而是一个：

> 轻量级浏览器调试和验证面板

它的价值在于：

- 你不需要每次都手写 curl
- 能快速验证鉴权是否生效
- 能验证部署后接口是否正常

---

## 八、当前项目的限制

现在这个项目还是“第一阶段骨架”，所以有这些限制：

### 1. 还没有真实模型接入

目前 provider 是：

```text
StubLlmProviderClient
```

所以 chat 返回的是模拟结果。

### 2. 还不支持流式输出

`stream=true` 会返回：

```text
stream_not_supported
```

因为真正的流式输出需要实现 SSE。

### 3. 还没有多渠道调度

虽然结构上已经有：

- route
- provider client
- model catalog

但目前只有一个 stub provider，没有做：

- 多 provider 切换
- 负载均衡
- 失败重试
- 配额路由

---

## 九、下一步最值得做什么

从项目演进顺序上，最建议继续做：

### 第一优先级

1. 接入真实的 OpenAI-compatible Provider
2. 实现 `stream=true` 的 SSE 响应
3. 把管理页增强为真正的渠道/状态面板

### 第二优先级

4. 增加调用日志
5. 增加多 provider / 多 key 路由
6. 增加 HTTPS / 反向代理部署说明

---

## 十、上下文文档

项目额外维护了一个上下文 handoff 文档：

```text
docs/context.md
```

这个文件是为了后续 Claude Code / 新会话继续接手时，能够快速知道：

- 项目目标
- 当前状态
- 部署约定
- 下一步计划

这里不应该存放：

- 真实 API Key
- 服务器 IP
- 私有账号信息
- Cookie / Token
- 敏感运维信息

---

## 十一、总结

如果你把这个项目理解成一句话，就是：

> 这是一个“OpenAI 兼容网关”的第一版脚手架，已经具备接口、鉴权、管理页和 Docker 部署能力，但还没有接真实模型平台。

如果你愿意，下一步我可以继续做两件事里的一个：

1. **把这些中文注释继续补到更多 Java 文件里**
2. **开始接入真实 Provider，让 chat 返回不再是 stub**
