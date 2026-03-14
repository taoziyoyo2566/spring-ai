# Spring-Claw

Spring-Claw 是一个基于 Spring Boot + Spring AI 的对话服务示例，支持按会话记忆检索与存储，使用 PostgreSQL + pgvector 作为向量数据库，默认对接 Ollama 作为本地模型服务。

## 功能概览
- 会话级记忆：按 `sessionId` 进行向量检索和隔离
- 自动记忆写入：将用户输入写入向量库
- 统一错误响应：参数校验与异常处理统一格式
- 简单鉴权：HTTP Basic 保护 API
- 多环境配置：`dev` / `prod` / `test`

## 技术栈
- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.x
- PostgreSQL + pgvector
- Ollama

## 快速开始

1. 准备 PostgreSQL 并启用 pgvector 扩展  
   Flyway 会在启动时执行迁移。注意：创建扩展需要数据库用户具备相应权限。
2. Ollama 已由上层目录 `../ollama` 的 `docker-compose.yml` 启动，无需重复启动  
   确认接口可用：`http://127.0.0.1:11434`
3. 配置环境变量
4. 启动应用

```bash
mvn spring-boot:run
```

## 环境变量

可以参考 `env.example` 配置。

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 运行环境 | `dev` |
| `DB_HOST` | 数据库地址 | `localhost` |
| `DB_PORT` | 数据库端口 | `5432` |
| `DB_NAME` | 数据库名称 | `openclaw_memory` |
| `DB_USERNAME` | 数据库用户名 | `openclaw` |
| `DB_PASSWORD` | 数据库密码 | `openclaw_password` |
| `OLLAMA_BASE_URL` | Ollama 服务地址 | `http://localhost:11434` |
| `API_USERNAME` | API 基本认证用户名 | `springclaw` |
| `API_PASSWORD` | API 基本认证密码 | `dev_secret` |
| `EMBEDDING_DIMENSIONS` | 向量维度 | `768` |

可选配置（按需添加）：
- `spring.ai.ollama.chat.options.model` 指定聊天模型
- `spring.ai.ollama.embedding.options.model` 指定嵌入模型

## API 使用

基础认证：`API_USERNAME` / `API_PASSWORD`

### 健康检查
```bash
curl http://localhost:8080/actuator/health
```

### 发送对话
```bash
curl -u springclaw:dev_secret -X POST \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo","userInput":"你好"}' \
  "http://localhost:8080/api/spring-claw/chat"
```

返回值为模型生成的纯文本内容。

## 数据库说明

迁移文件位于 `src/main/resources/db/migration`。
`V2__create_vector_store.sql` 使用 Flyway 占位符 `vector_dimensions`，其值来自 `EMBEDDING_DIMENSIONS`。默认与 `nomic-embed-text` 的 768 维一致。  
一旦数据库已初始化，若需要修改维度，请新增迁移并调整 `vector_store.embedding` 列类型。

## 测试

```bash
mvn test
```

测试使用 H2 内存库，并通过 Mock 屏蔽外部依赖（VectorStore / ChatClient）。

## 目录结构

- `src/main/java` 业务代码
- `src/main/resources` 配置与迁移
- `src/test` 测试

## License

Apache-2.0
