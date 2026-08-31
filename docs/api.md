# Talk-Code 前后端接口文档

> 后端接口定义于 `src/main/java/com/talkcode/controller`；前端调用层由 openapi2ts 自动生成于 `talk-code-frontend/src/api/`（`npm run openapi2ts` 从 `/api/v3/api-docs` 生成，勿手改）。

---

## 0. 全局约定

| 项 | 值 |
|---|---|
| 后端端口 | `9011`，context-path = **`/api`**（`application.yaml`） |
| 前端 baseURL | `/api`（dev 由 Vite 代理到 `localhost:9011` 且**不重写路径**；prod 由 Nginx 反代） |
| 凭证 | Cookie 会话（axios `withCredentials: true`；Session 存 Redis，30 天） |
| 响应拦截 | 前端 `request.ts`：业务码 `40100`（未登录）自动跳 `/user/login?redirect=...` |

### 通用返回结构 `BaseResponse<T>`（`common/BaseResponse.java`）

```json
{ "code": 0, "data": T, "message": "ok" }
```

### 错误码（`exception/ErrorCode.java`）

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40050 | 请求频率过快（SSE 限流触发） |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40300 | 禁止访问 |
| 40400 | 数据不存在 |
| 50000 | 系统内部异常 |
| 50001 | 操作失败 |

### 通用请求体

- `DeleteRequest`：`{ id }`
- `PageRequest`（各 QueryRequest 的父类）：`{ pageNum=1, pageSize=10, sortField, sortOrder="descend" }`
- 分页返回 MyBatis-Flex `Page<T>`：`{ records, pageNumber, pageSize, totalPage, totalRow }`

---

## 1. 应用模块 AppController（`/app`）

> 后端：`controller/AppController.java`；前端：`api/appController.ts`（12 个函数）。

| 方法 | 路径 | 说明 | 请求参数 / Body | 返回 data |
|---|---|---|---|---|
| GET | `/app/chat/gen/code` | **SSE 对话生成代码**（核心） | Query：`appId`(必填)、`message`(必填)、`mode`(可选 `classic`/`workflow`)；限流 5 次/60 秒 | SSE 流（见第 7 节） |
| POST | `/app/add` | 创建应用（AI 自动起名+路由类型） | `{ initPrompt }` | `Long`（appId） |
| POST | `/app/update` | 更新应用名（仅本人） | `{ id, appName }` | `Boolean` |
| GET | `/app/download/{appId}` | 下载项目代码 zip（仅创建者） | Path：`appId` | **zip 文件流（非 BaseResponse）** |
| POST | `/app/deploy` | 部署应用 | `{ appId }` | `String`（部署 URL） |
| POST | `/app/delete` | 删除应用（本人或管理员） | `{ id }` | `Boolean` |
| GET | `/app/get/vo` | 获取应用详情 | Query：`id` | `AppVO` |
| POST | `/app/my/list/page/vo` | 我的应用分页（强制 userId=当前用户，pageSize≤20） | `AppQueryRequest` | `Page<AppVO>` |
| POST | `/app/good/list/page/vo` | 精选应用分页（强制 priority 精选，走缓存） | `AppQueryRequest` | `Page<AppVO>` |
| POST | `/app/admin/delete` | 管理员删除应用 `@AuthCheck(admin)` | `{ id }` | `Boolean` |
| POST | `/app/admin/update` | 管理员更新（名称/封面/优先级） | `{ id, appName, cover, priority }` | `Boolean` |
| POST | `/app/admin/list/page/vo` | 管理员应用分页 | `AppQueryRequest` | `Page<AppVO>` |
| GET | `/app/admin/get/vo` | 管理员获取应用详情 | Query：`id` | `AppVO` |

**DTO/VO 字段**

- `AppQueryRequest`（继承 PageRequest）：`id, appName, cover, initPrompt, codeGenType, deployKey, priority(Integer), userId`
- `AppVO`：`id, appName, cover, initPrompt, codeGenType, deployKey, deployedTime, priority, userId, createTime, updateTime, user(UserVO)`

---

## 2. 对话历史模块 ChatHistoryController（`/chatHistory`）

> 后端：`controller/ChatHistoryController.java`；前端：`api/chatHistoryController.ts`（3 个函数）。

| 方法 | 路径 | 说明 | 请求参数 | 返回 data |
|---|---|---|---|---|
| GET | `/chatHistory/app/{appId}` | 按应用查历史（**游标分页**） | Path：`appId`；Query：`pageSize`(默认 10)、`lastCreateTime`(LocalDateTime，上一页最后一条时间) | `Page<ChatHistory>` |
| GET | `/chatHistory/turn/{turnId}/events` | 按轮次查事件日志（管理员或应用创建者） | Path：`turnId` | `List<ChatEventLog>` |
| POST | `/chatHistory/admin/list/page/vo` | 管理员全量对话分页 | `ChatHistoryQueryRequest` | `Page<ChatHistory>` |

**实体字段**

- `ChatHistory`：`id, message, reasoningContent, messageType(user/ai), turnId, appId, userId, createTime, updateTime, isDelete`
- `ChatEventLog`：`id, appId, memoryId, turnId, seq, codeGenType, role, eventType, content, reasoningContent, toolCallId, toolName, toolArguments, toolResult, rawEventJson, userId, createTime, updateTime, isDelete`
- `ChatHistoryQueryRequest`（继承 PageRequest）：`id, message, messageType, appId, userId, lastCreateTime`

---

## 3. 用户模块 UserController（`/user`）

> 后端：`controller/UserController.java`；前端：`api/userController.ts`（10 个函数）。

| 方法 | 路径 | 说明 | 请求参数 / Body | 返回 data |
|---|---|---|---|---|
| POST | `/user/register` | 注册 | `{ userAccount, userPassword, checkPassword }` | `Long`（userId） |
| POST | `/user/login` | 登录（写 Session Cookie） | `{ userAccount, userPassword }` | `LoginUserVO` |
| GET | `/user/get/login` | 获取当前登录用户 | 无 | `LoginUserVO` |
| POST | `/user/logout` | 登出 | 无 | `Boolean` |
| POST | `/user/add` | 新增用户（admin，默认密码 12345678） | `{ userName, userAccount, userAvatar, userProfile, userRole }` | `Long` |
| GET | `/user/get/vo` | 查用户（脱敏 VO） | Query：`id` | `UserVO` |
| GET | `/user/get` | 查用户（admin，未脱敏） | Query：`id` | `User` |
| POST | `/user/delete` | 删用户（admin） | `{ id }` | `Boolean` |
| POST | `/user/update` | 改用户（admin） | `{ id, userName, userAvatar, userProfile, userRole }` | `Boolean` |
| POST | `/user/list/page/vo` | 用户分页（admin） | `UserQueryRequest`（PageRequest + `id, userName, userAccount, userProfile, userRole`） | `Page<UserVO>` |

**VO 字段**：`LoginUserVO` = `{ id, userAccount, userName, userAvatar, userProfile, userRole, createTime, updateTime }`；`UserVO` 同上（无 updateTime）。

---

## 4. 静态资源模块 StaticResourceController（`/static`）

| 方法 | 路径 | 说明 | 返回 |
|---|---|---|---|
| GET | `/static/{deployKey}/**` | 托管已生成/已部署应用的静态文件（根目录 `{user.dir}/tmp/code_output/{deployKey}`） | `ResponseEntity<Resource>`（**非 BaseResponse**；404 notFound；目录访问 301 补 `/`；默认回 `index.html`） |

- 前端实际用法：不经过 `api/staticResourceController.ts`，而是用 `config/env.ts` 的 `getStaticPreviewUrl(codeGenType, appId)` 拼 `${API_BASE_URL}/static/{codeGenType}_{appId}/`（Vue 项目追加 `dist/index.html`）作为 iframe 地址。

---

## 5. 健康检查 HealthController（`/health`）

| 方法 | 路径 | 返回 |
|---|---|---|
| 任意（GET/POST/PUT/DELETE/PATCH） | `/health/` | `BaseResponse<String>`（`"ok"`） |

> 前端 `api/healthController.ts` 生成了 5 个方法变体函数，页面未实际调用。

---

## 6. 工作流演示 WorkflowSseController（`/workflow`，前端未调用）

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| POST | `/workflow/execute` | Query：`prompt` | `WorkflowContext`（同步 JSON，非 BaseResponse） |
| GET | `/workflow/execute-flux` | Query：`prompt` | `Flux<String>`（text/event-stream 纯文本帧） |
| GET | `/workflow/execute-sse` | Query：`prompt` | `SseEmitter`（text/event-stream） |

> langgraph4j 工作流的演示接口，`talk-code-frontend/src/api/` 下无对应文件，前端未使用。

---

## 7. SSE 流式接口详解：`GET /api/app/chat/gen/code`

- **后端实现**：`AppController.chatToGenCode`，`produces = TEXT_EVENT_STREAM`，返回 `Flux<ServerSentEvent<String>>`。
- **前端实现**：`AppChatPage.vue`（及 admin/ChatManagePage.vue）用原生 `EventSource` 直连（GET），`withCredentials: true`；**不**走 axios。
- **限流**：`@RateLimit(USER, 5 次 / 60 秒)`，超限返回错误码 40050。
- **幂等**：前端生成 `clientRequestId`（时间戳+随机数）作请求参数防重复提交。

### 请求参数（Query String）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `appId` | long | 是 | 应用 ID |
| `message` | string | 是 | 用户消息（可含可视化编辑器选中的元素信息） |
| `mode` | string | 否 | `classic`（默认）/ `workflow`；该应用一旦用过 workflow 会被服务端锁定 |

### SSE 消息格式

**内容增量**（默认 `message` 事件）：

```
data: {"d":"<本次增量文本>"}
```

前端 `JSON.parse(event.data).d` 取出增量拼接渲染。

**结束事件**（流末尾追加）：

```
event: done
data:
```

前端监听 `done`：关闭连接、延迟 1s 刷新 iframe 预览。

**业务错误事件**（由 `GlobalExceptionHandler` 构造）：

```
event: business-error
data: {错误信息 JSON}
```

**连接错误**：`onerror` 中前端立即 `close()`，防止浏览器自动重连导致重复触发生成。

### 后端内部流消息协议（`ai/model/message/StreamMessageTypeEnum`）

后端 AI 层（尤其 Vue 工程智能体）内部产生 4 类 JSON 消息，由 `JsonMessageStreamHandler` 解析、渲染、聚合落库；对外 SSE 已统一简化为 `{"d": ...}`：

| type | 结构 | 含义 |
|---|---|---|
| `thinking` | `{type, data}` | 深度思考内容 |
| `ai_response` | `{type, data}` | AI 正式回复 |
| `tool_request` | `{type, id, name, arguments}` | 模型请求调用工具 |
| `tool_executed` | `{type, id, name, arguments, result}` | 工具执行结果 |

工具名约定（`ai/tools/`）：`writeFile`、`readFile`、`modifyFile`、`deleteFile`、`readDir`、`updatePlan`（生成计划，前端渲染为计划面板）、`exit`（结束）。

---

## 8. 前后端一致性核对结论

1. **路径一致性**：前端所有请求路径均不带 `/api` 前缀，由 axios baseURL（`/api`）+ 后端 context-path（`/api`）+ Vite 代理（不重写）三层配合，31 个前端函数全部与后端端点匹配，无路径/方法不一致项。
2. **后端存在但前端未调用**：`/workflow/execute`、`/workflow/execute-flux`、`/workflow/execute-sse` 三个演示端点。
3. **前端定义但页面未实际使用**：`healthController.ts` 的 5 个函数、`staticResourceController.ts` 的 `serveStaticResource`（页面用 `getStaticPreviewUrl` 拼直链）。
4. **特殊返回**：`/app/download/{appId}` 与 `/static/{deployKey}/**` 返回原始文件流（非 BaseResponse JSON），前端分别按 blob 下载 / iframe 直链处理。
5. **SSE 接口的 axios 封装**（`appController.ts` 的 `chatToGenCode`，类型标注为 `ServerSentEventString[]`）仅是 OpenAPI 生成的占位，实际页面由 EventSource 直连消费。
