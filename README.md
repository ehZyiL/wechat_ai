# Kn-AI-Chat 微信客服 AI 助手 (v2.0)

`kn-ai-chat` 是一个功能强大、可高度定制的微信 AI 客服助手。它基于 Java Spring Boot 框架构建，深度集成了企业微信的客服消息接口和多种先进的 AI 模型能力。

## 如果需要部署此项目，需要注意查看 **注意事项** 或者 `.env` 文件中的说明 - 这很重要！！！

https://github.com/user-attachments/assets/fc1c3d0d-9faa-40ef-9073-e48c567481a6

## ✨ 核心功能
该项目通过模块化的消息处理器（Handler）实现丰富的功能，并支持精细的用户级和全局级配置。

### 🧠 智能 AI 对话
- **上下文感知**: 能够理解并记忆对话历史，进行多轮对话，并且根据上下文内容判断是否启用语音回答。
- **模型可配**: 支持通过后台配置，为不同用户或全局设置不同的模型和系统提示词。

### 📚 动态知识库 (RAG)
- **即时学习**: 用户可直接发送 `.txt`, `.pdf`, `.docx`, `.csv` 等多种格式的文件，系统将自动解析并将其内容加入该用户的专属知识库。
- **精准问答**: 在回答问题时，可触发知识库模式，让 AI 基于您提供的文档内容进行回答。
- **文件管理**: 用户可通过指令（如 `列出文件`, `删除文件 <ID>`）或后台管理自己的知识库文件。

### 🗣️ 无缝人工对话
- **一键转接**: 用户可以通过关键词（如“转人工”）无缝切换到人工客服模式。
- **实时聊天界面**: 管理员可以在后台的“客服窗口”页面与用户进行实时文字、图片和文件交流。
- **会话管理**: 系统会自动管理待接入用户列表，并在管理员结束服务后自动切换回 AI 客服。

### 🌐 MCP - 模型协管平台
- **多模型管理**: 允许接入和管理多个外部模型服务（Model Copilot），并为它们创建独立的配置。
- **权限控制**: 管理员可以为每个用户精细地授权可以使用哪些 MCP 服务。
- **动态调用**: 用户可以通过特定指令（如 `#mcp-[序号] [问题]`）调用已授权的 MCP 服务进行对话。

### 🎨 AI 绘画
- **集成 SiliconFlow 的先进文生图模型**（如 `Kwai-Kolors/Kolors`）。
- **用户通过简单指令**（如 `画一张未来城市的风景`）即可触发 AI 进行绘画创作。
- **系统会自动处理图片压缩**，确保符合微信的发送限制。

### 🔊 多模态语音交互
- **语音转文本 (ASR)**: 自动识别用户发送的语音消息，并将其转换为文本进行处理。
- **文本转语音 (TTS)**: 支持将 AI 的文字回复合成为语音，并发送给用户，实现更自然的交互。
- **智能判断**: 可通过语义分析判断用户意图，自动选择是否使用语音进行回复。

### ⚙️ 强大的后台管理系统
- **用户管理**: 查看所有互动过的用户，进行拉黑/解封、清除对话历史、彻底删除用户等操作。
- **AI 配置中心**: 为全局或单个用户配置不同的 AI 模型、API Key、System Prompt 及其他多媒体模型参数。
- **MCP 服务管理**: 新增、删除和配置 MCP 服务，并管理用户对这些服务的访问权限。
- **关键词配置**: 为不同功能（如绘画、查彩票、菜单等）配置独立的触发关键词，支持用户级和全局级配置。
- **自定义回复**: 设置关键词和回复规则，实现高优先级的自动应答，支持完全匹配和包含匹配两种模式。
- **文件管理**: 查看和删除指定用户的知识库文件。
- **系统级操作**: 提供一键清除所有系统数据（用户、配置、日志、缓存）的操作。

### 🔧 高度可扩展的架构
- **处理器链**: 采用责任链模式，通过 `MessageHandler` 接口定义消息处理流程，优先级可控，易于新增功能。
- **服务化**: 核心功能（如 AI、微信、数据库、缓存）均被封装为独立的服务，逻辑清晰。
- **Docker 部署**: 提供 `docker-compose.yml`，通过环境变量注入配置，实现一键启动和便捷部署。

## 🛠️ 技术栈
- **后端**: Java 21, Spring Boot 3.5.0, Langchain4j
- **数据存储**:
  - **数据库**: H2, Spring Data JPA
  - **缓存/消息队列**: Redis (用于缓存微信 Access Token、消息去重等)
- **AI 服务**:
  - **模型接口**: SiliconFlow (默认)
  - **大语言模型**: `deepseek-ai/DeepSeek-V3` (默认)
  - **文生图模型**: `Kwai-Kolors/Kolors` (默认)
  - **语音模型**: `FunAudioLLM/SenseVoiceSmall` (ASR), `FunAudioLLM/CosyVoice2-0.5B` (TTS) (默认)
  - **多模态模型 (VLM)**: `Qwen/Qwen2-VL-72B-Instruct` (默认)
  - **RAG 嵌入模型**: `Qwen/Qwen3-Embedding-8B` (默认)
- **前端 (管理后台)**: Thymeleaf, Bootstrap 5
- **构建与部署**: Maven, Docker, Docker Compose
- **文件处理**: Apache POI (Word, Excel), Apache PDFBox (PDF), Jsoup (HTML), OpenCSV (CSV)
- **音视频处理**: JAVE (Java Audio Video Encoder)

## 📸 项目页面截图
以下是项目中各个页面的截图，展示其界面和功能：
### 管理员登录
![后台登录页面](files/login.png)
### 用户管理
![用户管理页面](files/user.png)
### 人工客服聊天（目前只支持文字）
![人工客服聊天页面](files/chat.png)
### MCP 服务权限管理
![MCP 服务权限管理页面](files/mcp-permissions.png)
### MCP 模型配置
![MCP 模型配置页面](files/mcp-config.png)
### AI 配置
![AI 配置页面](files/config.png)
### 关键词配置
![AI 关键词配置页面](files/keywords.png)
### 文件管理
![文件管理页面](files/files.png)
### 自定义回复配置
![AI 自定义回复页面](files/reply.png)

## 微信配置
### 首先从这里扫码登录
- https://kf.weixin.qq.com/kf/loginpage?redirect_uri=https%3A%2F%2Fkf.weixin.qq.com%2Fkf%2Fframe%3Ffrom%3Dauth#/config

### 点击开发配置

### 随机获取生成的 Token
- `WECHAT_TOKEN=`

### 随机获取生成的 EncodingAESKey
- `WECHAT_AES_KEY=`

### 点击企业信息 - 企业 ID
- `WECHAT_CORP_ID=`

### 接着启动项目
- 微信回调页面设置回调 URL 为: `http://ip:port/wechat/callback`
- 验证成功后会返回 Secret

### 回调配置成功的 Secret
- `WECHAT_SECRET=`

### 点击客服账号 - 选择客服 - 账号 ID
- `WECHAT_OPEN_KFID=`

### 管理后台密码 - 默认为 xlike
- `ADMIN_PASSWORD=xlike`

### AI 服务商的 API Key - 最好使用 SiliconFlow，别的厂商没测试过!!!
- `AI_API_KEY=sk-`

### Docker 端口
- `DOCKER_SERVER_PORT=8081`

## 🚀 部署与使用
### 直接下载仓库中的 `docker-compose.yml` 和 `.env` 文件，运行以下命令启动：
```
docker-compose up -d
```

## 📝 注意事项（必看！！！）
- 确保 Redis 服务正常运行。
- 项目启动成功之后，拿到微信的 Secret 需要停止项目，重新在 `.env` 文件中配置，并且重新启动。

## 第一次打开客服聊天面板入口
- https://kf.weixin.qq.com/kf/frame?from=auth#/start
- 开始接入
- 在微信外 App/网页中接入 - 去接入
- 拿到链接，点击对话
- 后面你就可以直接在微信分享客服，就没这么麻烦了