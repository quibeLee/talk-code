# Talk-Code 项目整体架构文档

> Talk-Code 是一个 **AI 应用生成平台**：用户通过自然语言对话，让 AI 生成可运行的网站应用（HTML 单页 / 多文件网站 / Vue 工程），支持实时预览、可视化编辑、一键部署与代码下载。

---

## 1. 总体形态

```
┌─────────────────────────────┐         ┌──────────────────────────────────────────┐
│  前端 talk-code-frontend     │         │  后端 Spring Boot (localhost:9011, /api) │
│  Vue 3 + TS + Antd + Pinia  │  HTTP/  │                                          │
│                             │ ──SSE─► │  Controller → Service → 生成引擎          │
│  EventSource 消费 SSE 流     │         │    ├─ Classic：langchain4j AiServices    │
│  iframe 预览生成结果          │ ◄────── │    └─ Workflow：langgraph4j 节点链        │
│  visualEditor 可视化编辑      │ /api    │                                          │
└─────────────────────────────┘         │  MySQL(业务) + Redis(会话/记忆/缓存/限流)   │
         ▲  iframe 加载                  │  COS(封面/文件)  Prometheus(监控)         │
         └── /api/static/{deployKey}/ ──┤  静态目录 tmp/code_output/{deployKey}     │
                                        └──────────────────────────────────────────┘
```

- 前端所有请求走同源 `/api`：开发环境由 Vite 代理到 `localhost:9011`（不重写路径），生产由 Nginx 反代。这样 Cookie 会话可用，且可视化编辑器能同源注入 iframe。
- AI 生成的站点由后端写入静态目录，前端通过 `/api/static/{deployKey}/` 用 iframe 预览。
- 对话内容通过原生 `EventSource`（GET SSE）流式传输。

---

## 2. 技术栈

### 后端（`pom.xml`，入口 `src/main/java/com/talkcode/TalkCodeApplication.java`）

| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 4.1.0、Java 21、SSE（`Flux<ServerSentEvent>` / `SseEmitter`） |
| AI 编排 | langchain4j 1.19（AiServices + TokenStream + ChatMemory + Guardrail）、**langgraph4j 1.6.0-rc2**（工作流编排）、阿里云 DashScope SDK（文生图 wan2.2-t2i-flash） |
| 数据层 | MySQL + **MyBatis-Flex** 1.11.8（注意不是 MyBatis-Plus）+ HikariCP |
| Redis | spring-session-data-redis（分布式 Session，30 天）、Redisson 3.50（限流/锁）、Redis ChatMemory（对话记忆）、spring-cache |
| 缓存 | Caffeine（本地缓存 AI 服务实例、精选应用列表） |
| 存储/工具 | 腾讯云 COS（封面/文件）、Selenium + webdrivermanager（网页截图）、jsoup（HTML 补丁解析）、Hutool |
| 文档/监控 | knife4j + springdoc（`/api/doc.html`）、Actuator + micrometer-prometheus（`/api/actuator/prometheus`） |

### 前端（`talk-code-frontend/package.json`）

| 分类 | 技术 |
|---|---|
| 框架 | Vue 3.5（`<script setup>`）+ TypeScript 5.8 + Vite 7 |
| UI/状态 | Ant Design Vue 4.x、Pinia（唯一 store：登录用户）、vue-router 4（history 模式） |
| 渲染 | markdown-it + highlight.js（AI 回复渲染） |
| 接口生成 | `@umijs/openapi`：`npm run openapi2ts` 读取后端 `/api/v3/api-docs` 自动生成 `src/api/*.ts`（勿手改） |
| 通信 | axios（`withCredentials`）+ 原生 EventSource（SSE） |

### AI 模型配置（`src/main/resources/application.yaml`，4 套模型分工）

| 模型 Bean | 供应商/型号 | 用途 |
|---|---|---|
| `chat-model` / `streaming-chat-model` | DeepSeek | HTML / 多文件生成 |
| `reasoning-streaming-chat-model` | DeepSeek（max-tokens 32768） | Vue 工程智能体（带工具调用） |
| `simple-task-chat-model` | DashScope qwen-turbo（兼容模式） | 轻任务：标题生成、代码类型路由 |

> 模型 Bean 均为 **prototype 多例**（`config/ChatModelConfig` 等），避免并发共享问题。

---

## 3. 后端包结构（`src/main/java/com/talkcode`）

```
com.talkcode
├── controller          # HTTP 入口（见 docs/api.md）
├── service / service.impl
│   ├── AppServiceImpl            # 核心编排：chatToGenCode、deployApp、创建应用（AI 起名+路由类型）
│   ├── ChatHistoryService        # 对话历史；loadChatHistoryToMemory() 把 DB 历史回灌 Redis 记忆
│   ├── ChatEventLogService       # 事件日志
│   ├── ChatMemoryReplayService   # 基于 chat_event_log 的事件回放
│   ├── ContextCompactionService  # 按 token 阈值自动摘要压缩上下文
│   ├── TurnFlushService          # 单轮结束统一落库（事务）
│   ├── ScreenshotService         # Selenium 截图 → COS 封面
│   └── ProjectDownloadService    # 项目打包 zip 下载
├── core                # 生成核心
│   ├── AiCodeGeneratorFacade     # 门面：按代码类型分发，判断"创建 vs 修改"
│   ├── engine                    # 策略接口 ChatCodeGenerationEngine
│   │     ├── ClassicChatCodeGenerationEngine   # 走 Facade
│   │     └── WorkflowChatCodeGenerationEngine  # 走 langgraph4j
│   ├── handler                   # SSE 流处理
│   │     ├── StreamHandlerExecutor             # 按类型选处理器
│   │     ├── JsonMessageStreamHandler          # Vue 工程：解析 4 类 JSON 消息
│   │     ├── SimpleTextStreamHandler           # HTML/多文件：纯文本透传
│   │     └── TurnAccumulator(Manager)          # 流式阶段内存聚合，不落库
│   ├── parser                    # CodeParserExecutor + Html/MultiFile 解析器
│   ├── saver                     # CodeFileSaverExecutor + 模板方法保存器
│   └── builder                   # VueProjectBuilder（npm install + build）
├── langgraph4j         # 工作流引擎
│   ├── CodeGenWorkflow           # 主工作流（构图 + 聊天链路）
│   ├── node                      # ImageCollector / PromptEnhancer / Router /
│   │                             # CodeGenerator / CodeQualityCheck / ProjectBuilder
│   │                             # （node/concurrent：并发图片收集 5 节点 + Aggregator）
│   ├── state                     # WorkflowContext + WorkflowStreamConsumerRegistry
│   ├── ai                        # 图片收集规划/执行、代码质检 AiService 工厂
│   └── tools                     # ImageSearchTool(Pexels) / UndrawIllustrationTool /
│                                 # MermaidDiagramTool / LogoGeneratorTool(DashScope)
├── ai                  # langchain4j AI 服务层
│   ├── AiCodeGeneratorServiceFactory   # Caffeine 缓存创建三类 AiServices（核心）
│   ├── AiCodeGenTitleServiceFactory    # AI 起名
│   ├── AiCodeGenTypeRoutingServiceFactory  # 代码类型路由
│   ├── guardrail                 # PromptSafetyInputGuardrail / RetryOutputGuardrail
│   ├── tools                     # 模型可调用文件工具：writeFile/readFile/modifyFile/
│   │                             # deleteFile/readDir/exit/updatePlan + PlanTracker
│   └── model                     # StreamMessage + StreamMessageTypeEnum（4 类消息）、
│                                 # HtmlPatchOperation / MultiFilePatchOperation
├── config              # RedisChatMemoryStoreConfig(装饰链) / 各模型配置 / RedisSession /
│                       # RedisCacheManager / Cors / CosClient / Json / 上下文压缩属性
├── annotation + aop    # @AuthCheck + AuthInterceptor（admin 鉴权切面）
├── ratelimter          # @RateLimit + RateLimitAspect（Redisson RRate）
├── monitor             # AiModelMonitorListener + AiModelMetricsCollector
│                       # （token 用量/耗时/错误，tag: userId/appId/modelName）
├── mapper              # MyBatis-Flex mapper + resources/mapper/*.xml
├── model               # entity(App/User/ChatHistory/ChatEventLog)、dto、vo、enums
├── common / exception / utils / manager(CosManager) / generator(代码生成器)
```

### 关键设计点

1. **AiServices 工厂 + Caffeine 缓存**（`ai/AiCodeGeneratorServiceFactory.java`）：按 `appId + 生成类型` 缓存 AI 服务实例；每个实例挂 `MessageWindowChatMemory`（id=appId，maxMessages=50，存储在 Redis，创建时从 `chat_history` 预热 20 条）。
2. **三种 AiService**：
   - `AiCodeGeneratorService`：HTML/多文件，无工具，`Flux<String>` 流式；
   - `AiCodeCreateService`：Vue 创建，reasoning 流式模型 + 最小工具集（writeFile/updatePlan/exit），最多 50 轮工具调用；
   - `AiCodeModifyService`：Vue 修改，开放全部文件工具。
3. **ChatMemory 装饰链**（`config/RedisChatMemoryStoreConfig.java`）：`Compacting（压缩旧工具结果）→ Sanitizing（清洗非法消息）→ Redis（STRING，ttl 3600）`。
4. **流式两阶段落库**：流式过程中只在内存聚合（`TurnAccumulator`），`onComplete/onError` 时由 `TurnFlushService` 事务写入 `chat_history`（user+ai 两条）与 `chat_event_log`（事件事实表）。

---

## 4. 核心调用链路

### 4.1 对话生成主链路（SSE）

```
用户发送消息
  │
  ▼
AppController.chatToGenCode   GET /api/app/chat/gen/code  (@RateLimit 5次/60s)
  │  校验登录 / 应用归属
  ▼
AppServiceImpl.chatToGenCode()
  │  ① 取 CodeGenTypeEnum（html / multi_file / vue_project）
  │  ② 生成模式选择：Redisson 锁 app:chat:mode:lock:{appId}，用过 workflow 则永久锁定 workflow
  │  ③ 插入用户消息 → 设置监控上下文 → TurnAccumulatorManager.startTurn()
  ▼
按 ChatGenModeEnum 选择引擎（Spring 注入 List<ChatCodeGenerationEngine>）
  │
  ├──【Classic】ClassicChatCodeGenerationEngine
  │     └→ AiCodeGeneratorFacade.generateAndSaveCodeStream()
  │          ├─ HTML/MULTI_FILE：Flux<String> 纯文本流 → 完成后 CodeParser → CodeSaver 落盘
  │          └─ VUE_PROJECT：TokenStream（工具调用+响应）→ JSON 消息流
  │                → 完成后 VueProjectBuilder 执行 npm install/build
  │
  └──【Workflow】WorkflowChatCodeGenerationEngine
        └→ CodeGenWorkflow.executeWorkflowForChat()   节点链：
             image_collector（AI 规划 + 并发调 Pexels/undraw/Mermaid/Logo 工具）
               → prompt_enhancer（提示词增强）
               → router（代码类型路由）
               → code_generator（内部复用 AiCodeGeneratorFacade，分片经
                  WorkflowStreamConsumerRegistry 透传）
               → code_quality_check（AI 质检）
                    ├─ fail ──→ 回 code_generator 重生成
                    ├─ build → project_builder
                    └─ skip_build → END
  │
  ▼
StreamHandlerExecutor.doExecute()
  ├─ VUE_PROJECT → JsonMessageStreamHandler（解析 thinking / ai_response /
  │                tool_request / tool_executed 四类 JSON，工具调用渲染为前端
  │                可展示文本，updatePlan 渲染为结构化计划）
  └─ HTML/MULTI_FILE → SimpleTextStreamHandler（纯文本透传）
  │
  ▼
onComplete/onError → TurnFlushService.flushSuccess/flushError（事务落库
  chat_history 两条 + chat_event_log 事件）
  → doFinally 清理监控上下文 → Controller 追加 SSE `done` 事件结束
```

前端（`AppChatPage.vue`）收到增量 `{"d":"..."}` 拼接渲染；收到 `done` 后延迟 1s 刷新 iframe 预览地址（`/api/static/{type}_{appId}/`，Vue 项目追加 `dist/index.html`）。

### 4.2 部署链路

```
AppServiceImpl.deployApp()
  → Vue 项目先构建 dist
  → 拷贝到 CODE_DEPLOY_ROOT_DIR/{deployKey}
  → StaticResourceController 托管访问（GET /api/static/{deployKey}/**）
  → 虚拟线程异步：ScreenshotService 截图 → 上传 COS → 更新 app.cover
```

### 4.3 上下文治理链路

- 会话记忆：Redis ChatMemory（appId 为 memoryId），DB 历史预热。
- 自动压缩：`ContextCompactionService.autoCompactIfNeeded()`，token 超阈值（默认 800000）时摘要压缩旧消息。
- 输入护轨：`PromptSafetyInputGuardrail` 拦截不安全输入；`SanitizingChatMemoryStore` 清洗非法消息结构。

---

## 5. 数据库（`sql/create_table.sql`）

| 表 | 用途 | 关键字段 |
|---|---|---|
| `user` | 用户 | userAccount 唯一、userRole(user/admin)、逻辑删除 |
| `app` | 应用 | initPrompt、codeGenType、deployKey 唯一、priority（=99 表示精选） |
| `chat_history` | 对话历史展示视图 | message(longtext)、reasoningContent、turnId；(appId, createTime) 游标索引 |
| `chat_event_log` | 事件回放事实表 | memoryId/turnId/seq/eventType/工具调用全量字段/rawEventJson；(turnId, seq) 索引 |

---

## 6. 横切能力

| 能力 | 实现 |
|---|---|
| 鉴权 | Redis Session + `@AuthCheck(admin)` 切面（`aop/AuthInterceptor`）+ 业务层属主校验 |
| 限流 | `@RateLimit`（Redisson RRate，按用户维度），对话接口 5 次/60 秒 |
| 监控 | micrometer-prometheus；`monitor/` 包监听 langchain4j 调用，按 userId/appId/modelName 打点 token 用量、耗时、错误数 |
| API 文档 | knife4j：`http://localhost:9011/api/doc.html` |
| 全局异常 | `GlobalExceptionHandler`（业务错误在 SSE 中以 `business-error` 事件下发） |

---

## 7. 前端架构（`talk-code-frontend/`）

### 7.1 目录结构

```
src/
├── main.ts / App.vue        # 入口：Pinia + Router + Antd 注册；BasicLayout 全局包裹
├── access.ts                # 全局路由守卫：先拉取登录用户；/admin/* 需 admin 角色
├── request.ts               # axios 实例：baseURL=/api、withCredentials、
│                            # 40100 未登录自动跳 /user/login?redirect=...
├── api/                     # openapi2ts 自动生成（appController/userController/
│                            # chatHistoryController/staticResourceController/healthController）
├── pages/
│   ├── HomePage.vue             # 创建入口 + 我的作品/精选案例列表
│   ├── user/UserLoginPage.vue / UserRegisterPage.vue
│   ├── admin/UserManagePage.vue / AppManagePage.vue / ChatManagePage.vue
│   └── app/AppChatPage.vue      # ★ 核心页（约 2000 行）：对话 + SSE + 预览 + 部署 + 下载
│       AppEditPage.vue
├── components/MarkdownRenderer.vue  # markdown-it + highlight.js，流式内容持续重渲
├── utils/visualEditor.ts            # 可视化编辑器
├── stores/loginUser.ts              # 唯一 Pinia store
└── config/env.ts                    # API_BASE_URL / 静态预览地址 / 部署域名
```

### 7.2 关键机制

1. **SSE 消费**（`AppChatPage.vue` generateCode()）：原生 `new EventSource('/api/app/chat/gen/code?...', { withCredentials: true })`；`onmessage` 解析 `{d:"增量"}` 拼接；监听自定义事件 `done`（结束并刷新预览）、`business-error`；`onerror` 立即 `close()` 防止浏览器自动重连导致重复生成。`clientRequestId`（时间戳+随机）用于幂等。
2. **流式渲染容错**：`mergeStreamSnapshotOrDelta()` 兼容快照式/增量式分片并去重；`parseStructuredStreamChunk()` 识别 thinking / 工具事件 JSON，渲染"过程时间线"与"工具卡片"；workflow 模式的 `updatePlan` 渲染为带依赖关系的"生成计划"面板；流结束后 `sanitizeAssistantContent()` 过滤中间噪音。
3. **可视化编辑器**（`utils/visualEditor.ts`）：向预览 iframe（同源）注入脚本，拦截 hover/click 生成 CSS selector + 元素信息，`postMessage` 回父页面（协议：`TOGGLE_EDIT_MODE`/`CLEAR_SELECTION` ↔ `ELEMENT_SELECTED`/`ELEMENT_HOVER`）；选中元素回填聊天输入框，用户描述修改后随消息发给 AI 实现定点改版。
4. **实时预览**：iframe 直接加载 `/api/static/{codeGenType}_{appId}/`（Vue 项目加载 `dist/index.html`），生成完成后刷新。
