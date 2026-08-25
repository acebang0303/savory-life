# 知味生活 · SavoryLife

融合 O2O 本地生活 + 内容社区 + AI Agent 智能助手的全栈平台。

## 技术栈

- Backend: Spring Boot 3.5, JDK 21 Virtual Threads, Spring AI 2.0, MyBatis-Plus 3.5
- AI: DeepSeek API, pgvector, RAG, ReAct Agent, NL2SQL, Mixed Recommendation
- Middleware: MySQL 8.0, Redis 7.x, RocketMQ 5.3, MongoDB 7.x
- Frontend: Vue 3, Element Plus, Pinia, Vite, uni-app

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.9+
- MySQL 8.0
- Redis 7.x
- RocketMQ 5.3
- PostgreSQL + pgvector
- MongoDB 7.x

### 启动主应用
```bash
cd savory-modules
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
```

### 启动AI服务
```bash
cd savory-ai
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
```

### 访问接口文档
http://localhost:8080/swagger-ui/index.html
