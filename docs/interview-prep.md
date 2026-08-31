# AI Agent 开发岗位 · 面试准备文稿

> 项目：Talk Code AI —— AI 应用生成平台（对话生成 HTML / 多文件 / Vue 工程）
> 文稿结构：开场介绍 → 核心难点与解决方案（每题按"问题 → 方案 → 效果 → 追问预判"）→ 高频追问 Q&A → 加分项引导
> 文中标注的类名/路径均为项目真实代码位置，面试时可引用以证明深度参与。

---

## 一、开场介绍（30 秒版）

> "我做的是一个 **AI 应用生成平台**：用户用一句话描述需求，平台自动生成可运行的网站应用，支持 HTML 单页、多文件站点和完整的 Vue 工程。后端是 Spring Boot 4 + langchain4j + langgraph4j，核心是一条**多节点智能体工作流**——图片收集、提示词增强、类型路由、代码生成、AI 质检、工程构建，质检不通过会自动回环重新生成；Vue 工程模式下模型作为 Agent 自主调用 writeFile、updatePlan 等文件工具逐步搭建项目，最多 50 轮工具调用。我重点解决的是 **长对话上下文治理、工具调用可靠性、SSE 流式结构化输出、工作流状态传递和高并发下的幂等与隔离** 这几个问题。"

### 2 分钟展开版（在 30 秒版基础上补充）

- **双引擎设计**：标准模式（langchain4j AiServices 直连，低延迟）+ 工作流模式（langgraph4j 编排，高质量），按应用维度用 Redisson 锁定，避免同一应用两种模式混用导致上下文错乱。
- **四套模型分级**：DeepSeek 负责代码生成与推理（max-tokens 32768），DashScope qwen-turbo 负责标题生成、类型路由这类轻任务——**成本约降一个数量级**。
- **过程可视化**：统一定义了 thinking / ai_response / tool_request / tool_executed 四类流式消息协议，前端渲染生成计划面板和工具调用时间线。
- **可观测性**：实现 langchain4j 的 ChatModelListener 接入 Micrometer/Prometheus，按 userId/appId/modelName 打点 token 用量与耗时；另有一张 chat_event_log 事件溯源表支持单轮回放。

---

## 二、核心难点与解决方案（重点章节）

### 难点 1：长对话上下文治理 —— 记忆膨胀与工具结果爆炸

**问题**：代码生成是多轮长对话，一次 writeFile 的工具结果可能包含整个文件内容，几十轮下来上下文轻松超过模型窗口；同时 ChatMemory 存 Redis，序列化体积和 TTL 也要管控。

**方案**（四层治理，`config/RedisChatMemoryStoreConfig.java` + `service/ContextCompactionService.java`）：
1. **窗口截断**：`MessageWindowChatMemory`（memoryId=appId，maxMessages=50），只保留最近 N 条。
2. **装饰链压缩**：自定义 ChatMemoryStore 装饰链 `Compacting → Sanitizing → Redis`——Compacting 对**旧的工具调用结果做摘要压缩**（历史工具结果对后续生成价值低但体积最大），Sanitizing 清洗非法消息结构（防止不完整 tool_call 消息污染下次请求）。
3. **token 阈值自动压缩**：`ContextCompactionService.autoCompactIfNeeded()`，超过阈值（默认 800000，可按模型上下文 80% 配置）时用轻量模型对更早的历史做整体摘要，替换原始消息。
4. **冷热分离**：Redis 只做热记忆（TTL 3600s），全量历史落 MySQL `chat_history`；新建会话时从 DB **回灌最近 20 条预热**，Redis 失效也不丢上下文。

**效果**：长会话 token 消耗可控，不会因上下文超限中断；多轮修改场景下模型仍能记住早期需求。

**追问预判**：
- *摘要会丢关键信息怎么办？* → 压缩只作用于"旧的、被窗口淘汰边缘"的消息，最近消息永远原样保留；且需求关键信息在首轮用户消息里，压缩时对用户消息加权保留（可在话术里说"保留了原始事件日志 chat_event_log，必要时可回放重建"）。
- *为什么自己写装饰链而不是用框架的？* → langchain4j 的 ChatMemoryStore 是接口，装饰器模式可以逐层叠加且可单测，比改框架源码可控。

---

### 难点 2：AI 智能体工具调用的可靠性 —— 幻觉、死循环、过程失控

**问题**：让模型自主调用文件工具有三个经典风险：① 幻觉出不存在的工具名或参数结构错误；② 无限循环调用不退出；③ 过程是黑盒，用户看不到进展，也无法做计划级控制。

**方案**（`ai/tools/`、`ai/AiCodeGeneratorServiceFactory.java`）：
1. **工具注册与幻觉兜底**：`BaseTool` 抽象 + `ToolManager` 自动注册；对模型幻觉出的未注册工具名做**兜底策略**（返回友好错误引导模型纠正），另外处理了部分非标准 OpenAI 兼容厂商**返回体缺失 tool name 字段**的问题（按 callId 关联补齐）。
2. **调用轮次上限**：`maxToolCallingRoundTrips = 50`，硬性防死循环。
3. **显式退出工具**：注册 `exit` 工具，模型完成所有文件写入后主动调用结束，配合提示词约束"完成必须 exit"。
4. **计划工具 updatePlan**：要求模型先产出结构化生成计划（items + deps 依赖），每次执行通过 updatePlan 更新状态——既给用户可视化的进度条，也**强迫模型做规划再执行**（plan-and-execute 模式），显著减少无目的调用。
5. **输入护轨**：`PromptSafetyInputGuardrail` 拦截不安全输入；`RetryOutputGuardrail` 对格式不合格的输出自动重试。

**效果**：Vue 工程模式下模型可稳定连续写 10+ 个文件完成完整工程，过程全程可视化。

**追问预判**：
- *50 轮怎么定的？* → 经验值 + 观测：典型 Vue 工程 10~20 个文件，每文件 1~2 轮，50 留了余量；超限会作为失败终止并落库。
- *如何测试工具调用的稳定性？* → 结构化输出的质检节点 + 事件日志回放，可以离线复盘每一轮的 tool_request/arguments/result。

---

### 难点 3：流式 + 结构化消息的 SSE 协议设计

**问题**：Vue 模式下 TokenStream 里交织着 AI 文本、thinking、工具请求、工具结果；HTML 模式又是纯文本流。前端既要做打字机效果，又要渲染计划面板和工具卡片，协议不统一会非常混乱。

**方案**（`ai/model/message/`、`core/handler/`）：
1. 定义统一消息模型 `StreamMessage` + `StreamMessageTypeEnum`（**thinking / ai_response / tool_request / tool_executed** 四类）。
2. **TokenStream → Flux<String> 适配**：把 langchain4j 回调（onPartialResponse/onToolExecuted 等）转成 Reactor Flux 的 JSON 消息流；HTML/多文件模式走 `SimpleTextStreamHandler` 纯文本透传，Vue 模式走 `JsonMessageStreamHandler` 结构化解析。
3. **对外简化**：Controller 层 SSE 统一包装为 `{"d": "增量"}`，流末尾追加 `done` 事件，业务错误走 `business-error` 事件——前端 EventSource 只需处理三种情况。
4. **两阶段落库**：流式过程只在内存 `TurnAccumulator` 聚合，`onComplete/onError` 时由 `TurnFlushService` **事务性**写入 chat_history（user+ai 两条）与 chat_event_log（事件明细）——避免边流边写 DB 造成半条记录。

**效果**：一套协议支撑三种代码形态的前端渲染；任何时刻断开都不会写脏数据。

**追问预判**：
- *为什么 SSE 不是 WebSocket？* → 生成场景是单向服务端推送，SSE 基于原生 EventSource 更轻、自动重连语义可控（前端在 onerror 主动 close，防止浏览器重连导致**重复触发生成**），且 GET + Cookie 在网关/代理下兼容性好。
- *断线怎么办？* → 请求带 `clientRequestId` 幂等标识；事件已全量落 chat_event_log，理论可支持断点回放（可作为演进方向讲）。

---

### 难点 4：langgraph4j 工作流中"流式回调"的跨节点传递

**问题**：工作流状态（MessagesState）需要可序列化，但 SSE 的流式消费者（连接/Emitter）是**不可序列化的瞬时对象**；代码生成节点在链路深处，如何把它的增量分片实时透传给最外层的 SSE 连接？

**方案**（`langgraph4j/state/WorkflowStreamConsumerRegistry.java`、`CodeGenWorkflow.java`）：
1. 状态里只存一个 **streamSessionId**（字符串），真实的 consumer 注册在内存 Registry 中。
2. 各节点通过 streamSessionId 从 Registry **恢复** consumer，把代码分片透传出去；工作流由**虚拟线程**驱动执行链路。
3. `CodeGeneratorNode` 内部复用 `AiCodeGeneratorFacade`，保证经典引擎和工作流引擎的生成逻辑一致（DRY）。

**效果**：工作流任意节点产生的增量都能实时推到前端，状态对象保持可序列化。

**追问预判**：
- *为什么不用 ThreadLocal 传？* → langgraph4j 节点可能在不同线程/异步边执行，ThreadLocal 不可靠；注册表 + 会话 ID 是显式的、可控生命周期的方案（工作流结束清理）。
- *registry 内存泄漏？* → 流结束（done/异常）时移除，生命周期与 SSE 连接对齐。

---

### 难点 5：生成质量闭环 —— 质检节点 + 条件边回环

**问题**：LLM 生成的代码可能不完整、跑不起来，如何在不人工干预的情况下自动兜底？

**方案**（`langgraph4j/node/CodeQualityCheckNode.java` + `CodeGenWorkflow.createWorkflow()` 条件边）：
1. 新增 **AI 质检节点**：用结构化输出（QualityResult：是否通过 + 问题列表）评估生成结果。
2. 工作图条件边：`质检 fail → 回到 code_generator 重新生成（携带问题反馈）`；通过且需要构建 → `project_builder`（npm install/build）；无需构建 → END。**重生成有次数上限**，防止无限循环烧 token。
3. 解析层容错：`CodeParserExecutor` 对模型输出的代码块做鲁棒提取（兼容多余说明文字、不完整围栏等）。
4. Vue 修改场景支持 **HTML patch（CSS selector 定点修改）**：`HtmlPatchOperation` 让模型输出"改哪个元素、怎么改"的补丁而非整页重写，减少改错范围。

**效果**：一次通过率提升，失败请求自动自愈；构建失败也能通过节点链路感知并反馈。

---

### 难点 6：并发与幂等 —— 多用户同时生成

**问题**：代码生成是长耗时、有状态的操作，多人同时用会踩三类坑：共享模型实例的并发串扰、同一应用重复提交、AI 服务实例频繁创建。

**方案**：
1. **模型 Bean 全部 prototype 多例**（`config/ChatModelConfig` 等），避免单例流式模型被多请求共享状态（这是实际踩过并修复的 bug）。
2. **AiService 实例 Caffeine 缓存**：按 `appId + 生成类型` 缓存（`AiCodeGeneratorServiceFactory`），同一应用的对话复用同一实例（与其 ChatMemory 绑定），不同应用天然隔离。
3. **模式锁**：Redisson 锁 `app:chat:mode:lock:{appId}`，应用一旦用过 workflow 就永久锁定，防止两种引擎交替使用弄脏记忆。
4. **入口限流**：`@RateLimit`（Redisson RRate，按用户 5 次/60 秒），防止刷爆 token。
5. **幂等**：前端生成 `clientRequestId`，配合后端对进行中任务的校验，防止 EventSource 异常重连导致重复生成。

**效果**：支撑多用户并发生成，无串话、无重复任务。

---

### 难点 7：多工具并行编排 —— 图片收集

**问题**：工作流要为网站收集四类素材（Pexels 实拍图、undraw 插画、Mermaid 架构图、Logo 文生图），串行执行太慢（尤其文生图秒级到十秒级）。

**方案**（`langgraph4j/node/ImageCollectorNode.java`，及 `node/concurrent/` 并发变体）：
1. 先用轻量模型做**收集规划**（ImageCollectionPlanService：需要哪几类图、各几张），避免盲目全量调用。
2. `CompletableFuture` **并行**调用四个工具，聚合去重后写入 WorkflowContext（imageList）。
3. 提供了 **fan-out / aggregator** 的并发工作流变体（image_plan 扇出到 4 个 collector 节点后汇聚），以及子图变体，用于对比不同编排方式。

**效果**：素材收集耗时从串行叠加变为最慢单工具的耗时。

---

### 难点 8：LLM 应用可观测性与成本控制

**问题**：AI 应用上线后必须回答三个问题：token 花在哪、慢在哪、错在哪。

**方案**（`monitor/` 包）：
1. 实现 langchain4j `ChatModelListener`，接入 Micrometer → Prometheus：请求计数、错误计数、耗时 Timer，**tag 带 userId/appId/modelName**，可下钻到"哪个用户的哪个应用在哪个模型上花了多少 token"。
2. **事件溯源表** `chat_event_log`：memoryId/turnId/seq/eventType/工具调用全字段/rawEventJson，任何一轮生成可完整回放——既是审计也是调试利器。
3. **成本三板斧**：轻任务用 qwen-turbo（标题/路由）、上下文压缩减少重复 token、Caffeine 缓存 AI 服务实例与精选页（减少重复计算与调用）。

---

### 难点 9（前端侧，可简讲）：流式渲染与可视化编辑

- **流式渲染容错**：分片兼容"快照式/增量式"两种（`mergeStreamSnapshotOrDelta`：新分片以旧内容为前缀则取差值，否则追加），去重防丢失；`updatePlan` 拦截渲染成带依赖缩进的计划面板；流结束后 `sanitizeAssistantContent` 过滤工具 JSON 噪音。
- **可视化编辑**：向同源 iframe 注入脚本，拦截 hover/click 生成 CSS selector + 元素信息，`postMessage` 回父页面（TOGGLE_EDIT_MODE ↔ ELEMENT_SELECTED 协议）；选中元素随消息发给 AI 实现定点改版。这也是**为什么强制前端走同源 /api 代理**的原因（要拿到 iframe 的 contentDocument）。

---

## 三、高频追问 Q&A 速查

| 追问 | 回答要点 |
|---|---|
| 为什么用 langgraph4j？和 LangChain 比呢？ | 需要的是**图编排**（条件边回环、fan-out 并行、子图），不是链式调用；langgraph4j 是 LangGraph 的 Java 实现，与 Spring/langchain4j 技术栈无缝。LangChain 偏链式 + 生态在 Python 侧，团队是 Java 栈。 |
| Agent 的"记忆"怎么设计的？ | 三层：Redis 热记忆（MessageWindow 50 条 + 装饰链）→ MySQL 全量历史（chat_history）→ 事件明细（chat_event_log）可回放。预热回灌保证 Redis 失效可恢复。 |
| 如何防止 Agent 失控（死循环/乱写文件）？ | 工具白名单注册 + 幻觉兜底、50 轮上限、exit 工具显式退出、updatePlan 强制规划、路径约束在应用目录内、输入护轨。 |
| 结构化输出怎么保证？ | langchain4j 结构化提示 + `RetryOutputGuardrail` 自动重试 + 质检节点二次校验。 |
| 上下文压缩的触发时机？ | 每轮写入前估算 token，超过阈值（模型窗口 80%）触发摘要压缩；压缩粒度是"旧消息段"，最近窗口永不压缩。 |
| 生成的代码跑不起来怎么办？ | 三道防线：解析容错 → AI 质检回环重生成 → 真实 npm build 验证（VueProjectBuilder），失败落库可追溯。 |
| Token 成本怎么控？ | 模型分级（qwen-turbo 干轻活）、上下文压缩、窗口截断、限流、缓存，Prometheus 按 userId/appId/modelName 维度核算成本。 |
| 一句话讲清你们和"套壳 GPT"的区别？ | 我们做的是**工程化的 Agent 系统**：多节点工作流、工具调用可靠性、上下文治理、质量闭环、可观测性——模型只是其中一个可替换组件。 |
| 项目里你最自豪的设计？ | 建议选：ChatMemory 装饰链（可插拔的上下文治理）或 streamSessionId 注册表（工作流与流式输出的解耦），讲清"问题-取舍-效果"。 |
| 如果重做一次会改什么？ | ① 事件表基础上做断点续传/任务恢复；② 质检引入可执行验证（单测/类型检查）而非纯 LLM 判断；③ 上下文压缩做增量索引而非整段摘要。 |

---

## 四、可以主动引导的加分点

1. **虚拟线程**：工作流驱动、部署后异步截图均用 Java 21 虚拟线程，讲"IO 密集型任务的低成本并发"。
2. **事件溯源思想**：chat_history 是"展示视图"，chat_event_log 是"事实表"，支持回放/审计/续传演进。
3. **提示词工程**：11 个系统提示词模板分场景维护（html/multi-file/vue × 创建/修改、路由、标题、质检、图片规划）；Vue 修改走 CSS selector patch 而非整页重写。
4. **国产模型兼容踩坑**：非标准 OpenAI 兼容返回体（tool call 缺 name 字段）的适配兜底——体现对多模型接入的理解。
5. **规范落地**：Spring Boot 4 + JDK 21 新版本栈、接口 OpenAPI 自动生成前端 TS（openapi2ts）、SSE 协议文档化（docs/api.md）。

---

## 五、表达建议

- 每个难点按 **"现象/坑 → 方案 → 取舍 → 数据/效果"** 讲，控制在 1.5~2 分钟；
- 主动抛出"取舍"（为什么 SSE 不是 WebSocket、为什么注册表不是 ThreadLocal、为什么装饰链不是改框架），把面试官引到你熟悉的领域；
- 数字要能自洽：50 轮工具上限、窗口 50 条、限流 5 次/60s、压缩阈值 80% 窗口、预热 20 条——被追问时说出"经验值 + 观测调整"的依据即可。
