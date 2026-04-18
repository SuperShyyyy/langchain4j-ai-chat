# LangChain4jDemo

## 1. 项目介绍
这是一个基于 Spring Boot 的后端聊天服务，形态上类似 ChatGPT：
- 支持用户注册/登录（JWT 鉴权）
- 支持多会话（新建会话、会话列表、会话详情、删除会话）
- 支持流式聊天回复（SSE，`text/event-stream`）
- 支持会话历史持久化到 MySQL
- 支持对话上下文记忆存到 Redis（LangChain4j ChatMemory）
- 本质上是“业务后端 + 大模型 API 调用层”的组合，不是本地部署大模型。
- 前端内容请跳转 https://github.com/SuperShyyyy/langchain4j-ai-chat-Vue
## 2. 技术栈与关键依赖
- Web 框架：Spring Boot 3
- AI 框架：LangChain4j（OpenAI starter + Reactor）
- 数据库：MySQL + MyBatis-Plus
- 缓存/记忆：Redis（`RedisChatMemoryStore`）
- 鉴权：JWT（`jjwt`）
- 密码加密：BCrypt（`PasswordEncoder`）
依赖位置：`pom.xml`
## 3. 代码分层和职责

### 3.1 启动与基础配置

- `LangChain4jDemoApplication`
  - 启动 Spring Boot
- `application.yml`
  - 数据源、Redis、JWT、聊天记忆参数等配置入口
- `application-secret.yml` 需要单独创建并填入密钥配置
-  需要自行创建数据库  

### 3.2 配置层

- `APIConfig`
  - 读取大模型 API 的 `apiKey/baseUrl/modelName`
  - 目前主要提供 getter，属于“参数容器”
- `ConsultantConfig`
  - 创建 LangChain4j 的 `ChatMemoryProvider`
  - 每个 memoryId 对应一个消息窗口，存储后端用 Redis
- `AppJwtProperties` / `AppChatProperties`
  - 绑定 `app.jwt`、`app.chat` 配置
- `WebMvcConfig`
  - 注册 JWT 拦截器
  - 拦截 `/chat/**` 和 `/auth/me`
  - 放行 `/auth/login`、`/auth/register`
- `PasswordConfig`
  - 提供 BCrypt 的 `PasswordEncoder`

### 3.3 控制层

- `AuthController`（`/auth`）
  - `POST /register`：注册并返回 token
  - `POST /login`：登录并返回 token
  - `GET /me`：当前用户信息（需登录）
- `ChatController`（`/chat`）
  - 会话管理接口：创建、列表、详情、删除
  - 聊天接口：`POST /chat/sessions/{sessionId}/messages`
    - 返回 `Flux<String>`，即流式文本片段
    - 先写入用户消息，再调用 AI，结束后落库 assistant 回复

### 3.4 服务层

- `AuthService` / `AuthServiceImpl`
  - 注册：校验用户名重复、密码加密、写用户表、签发 JWT
  - 登录：校验密码和用户状态、签发 JWT
  - 获取当前用户资料
- `ChatSessionService` / `ChatSessionServiceImpl`
  - 管理会话和消息的数据库行为
  - 做“会话归属校验”（防止越权访问他人会话）
  - 负责把数据库历史消息“回填”到 Redis Memory（prepareMemory）
- `ConsultantService`
  - LangChain4j 的 `@AiService` 接口
  - `chat(memoryId, message)` 调用流式模型
  - 当前写死了系统提示词：`你好`

### 3.5 数据访问层

- `SysUser` / `SysUserMapper` -> `sys_user`
- `ChatSession` / `ChatSessionMapper` -> `chat_session`
- `ChatMessage` / `ChatMessageMapper` -> `chat_message`
### 3.6 认证与上下文
- `JwtTokenService`
  - 生成 token（subject=userId）
  - 解析 token 得到 userId

- `Result<T>`
  - 普通接口统一返回 `{code,msg,data}`
  - 但流式聊天接口不走这个包装（直接 SSE 流）
- `GlobalExceptionHandler`
  - 参数校验异常、业务异常、系统异常统一转成 `Result`
  - `BizException` 的 401 会映射成 HTTP 401

## 4. 核心业务流程

### 4.1 注册/登录
1. 前端调用 `/auth/register` 或 `/auth/login`
2. `AuthController` -> `AuthServiceImpl`
3. 注册时密码用 BCrypt 加密后存库
4. 登录成功后生成 JWT，返回给前端
5. 前端后续请求携带 `Authorization: Bearer <token>`
### 4.2 访问受保护接口
1. 请求进入 `JwtAuthInterceptor`
2. token 解析出 userId
3. 写入 `UserContext`
4. 业务代码通过 `UserContext.requireCurrentUserId()` 获取用户
### 4.3 会话创建与消息历史
1. 创建会话写入 `chat_session`
2. 用户发消息后写入 `chat_message`（role=user）
3. 获取会话详情时，按 `message_order` 正序返回历史
4. 删除会话是逻辑删除（`chat_session.deleted=1`）

### 4.4 流式聊天

1. `ChatController.chat` 收到消息
2. `prepareMemory(userId, sessionId)`：
   - 若 Redis 已有 memory，直接复用
   - 若没有，则把 MySQL 历史消息转成 LangChain4j message 列表回填 Redis
3. 先保存用户消息到 DB
4. 调用 `ConsultantService.chat(memoryId, message)` 获取 `Flux<String>`
5. 流结束后，把 assistant 的完整回复保存到 DB
这一套保证了：
- Redis 负责“上下文窗口”（模型推理时用）
- MySQL 负责“长期历史”（可回放，可恢复）

## 5. 数据模型对应关系

- `sys_user`
  - 账户信息（用户名、密码哈希、状态）
- `chat_session`
  - 一个用户的一个会话
  - 标题、是否删除、最后消息时间
- `chat_message`
  - 会话中的每条消息
  - `role`（`user/assistant/system`）
  - `message_order` 保证顺序

## 6. 配置项

- `app.jwt.secret / expiration-seconds / issuer`
  - JWT 密钥、过期秒数、签发者
- `app.chat.memory-max-messages`
  - 单个会话上下文窗口最多保留多少条消息
- `app.chat.memory-ttl-days`
  - Redis 中会话记忆缓存存活天数
- `spring.datasource.*`
  - MySQL 连接参数
- `spring.data.redis.*`
  - Redis 连接参数

## 7 application-secret.yml

~~~yml
langchain4j:
  open-ai:
    streaming-chat-model:
      api-key: #填写自己的api-key
      base-url: #填写自己调用模型的baseUrl
      model-name: #模型名称
      log-requests: true 
      log-responses: true
    chat-model:
      api-key: #填写自己的api-key
      base-url: #填写自己调用模型的baseUrl
      model-name: #模型名称
      log-requests: true
      log-responses: true
logging:
  level:
    dev.langchain4j: debug
spring:
  data:
    redis:
      host: localhost #填写自己redis主机地址
      port: 6379 #填写自己redis主机暴露端口

ai-chat:
  db:
    username: #填写数据库用户名 默认root
    password: #填写数据库密码
    schema: #填写数据库名称
    host: #填写数据库主机地址
    port: #填写数据库主机地址暴露端口

app:
  jwt:
    secret: #jwt-secrt换成自己的签名密钥

~~~

## 8数据库Sql

~~~sql
create table chat_message
(
    id            bigint auto_increment
        primary key,
    user_id       bigint      not null,
    session_id    bigint      not null,
    role          varchar(16) not null,
    content       text        not null,
    message_order int         not null,
    created_at    datetime    not null
);

create index idx_chat_message_session_id
    on chat_message (session_id);

create index idx_chat_message_session_order
    on chat_message (session_id, message_order);

create index idx_chat_message_user_id
    on chat_message (user_id);

create table chat_session
(
    id              bigint auto_increment
        primary key,
    user_id         bigint            not null,
    title           varchar(128)      not null,
    deleted         tinyint default 0 not null,
    created_at      datetime          not null,
    updated_at      datetime          not null,
    last_message_at datetime          null
);

create index idx_chat_session_user_id
    on chat_session (user_id);

create index idx_chat_session_user_updated
    on chat_session (user_id, updated_at);

create table sys_user
(
    id            bigint auto_increment
        primary key,
    username      varchar(64)       not null,
    password_hash varchar(255)      not null,
    nickname      varchar(64)       null,
    status        tinyint default 1 not null,
    created_at    datetime          not null,
    updated_at    datetime          not null,
    constraint uk_sys_user_username
        unique (username)
);
~~~

