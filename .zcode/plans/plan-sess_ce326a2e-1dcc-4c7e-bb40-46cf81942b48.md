# 生成 GitHub README.md 计划

## 背景
- 仓库根目录目前**没有 README.md**（已确认）
- 敏感配置均在 gitignore 的 `application-local.yaml` / `application-prod.yaml` / `talk-code-frontend/.env.production` 中，README 只写占位符，**不泄露真实密钥**（application.yaml 中的数据库密码等也将以占位符形式呈现）
- 项目内容已完全掌握（此前已完成架构与接口文档梳理）

## 交付物
在仓库根目录创建 `README.md`（中文，面向 GitHub 展示），包含以下章节：

1. **标题与简介**：Talk Code AI —— AI 应用生成平台；一句话定位（对话生成 HTML/多文件/Vue 工程，实时预览、可视化编辑、一键部署）+ 技术徽章（JDK 21 / Spring Boot 4 / Vue 3 / License 占位）
2. **✨ 功能特性**：三大代码生成模式；标准/工作流双引擎（langgraph4j 节点链：图片收集→提示词增强→路由→生成→质检→构建）；AI 智能体工具调用（写文件/改文件/生成计划）；流式过程可视化（思考/工具时间线/生成计划面板）；可视化编辑（iframe 元素选中精准改版）；实时预览与一键部署（COS 封面截图）；代码打包下载；对话历史与事件回放；限流/鉴权/Prometheus token 监控
3. **🏗️ 技术栈与架构**：后端/前端/AI 模型分层表格 + GitHub 可渲染的 mermaid 架构图（前端 → /api → Classic/Workflow 双引擎 → MySQL/Redis/静态目录）
4. **🚀 快速开始**：
   - 环境要求：JDK 21、Maven 3.9+、Node.js 18+（建议 20）、MySQL 8、Redis 6+、可用的 DeepSeek / 阿里云 DashScope API Key
   - 步骤：① `sql/create_table.sql` 初始化 talk_code 库 ② 创建 `application-local.yaml` 覆盖密钥（给出模板片段）③ 后端 `mvn spring-boot:run`（端口 9011，context-path /api）④ 前端 `cd talk-code-frontend && npm install && npm run dev`（Vite 代理 /api）⑤ 访问 http://localhost:5173
5. **🔑 配置说明**：表格列出需配置项（langchain4j 四套模型 api-key、cos.client、pexels.api-key、dashscope.api-key、数据源/Redis），并说明 local profile 机制
6. **📖 使用指南**：全局模式切换（标准/工作流，工作流一次即锁定）、生成、可视化编辑、部署与下载的简要操作说明
7. **📂 目录结构**：根目录 + 后端关键包 + 前端 src 的树状说明
8. **📚 项目文档**：链接 docs/architecture.md、docs/api.md
9. **📡 监控与 API 文档**：`/api/actuator/prometheus`、`/api/doc.html`（knife4j）
10. **📄 License**：占位（提示按需补充）

## 注意事项
- 不写真实密码/密钥；截图区留占位提示（用户可后续补充）
- 纯新增文件，不改动任何代码