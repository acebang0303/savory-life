# 知味生活 · SavoryLife

融合 O2O 本地生活 + 内容社区 + AI Agent 智能助手的全栈平台。

## 技术栈

- Backend: Spring Boot 3.5, JDK 21 Virtual Threads, Spring AI 2.0, MyBatis-Plus 3.5
- AI: DeepSeek API, pgvector, RAG, ReAct Agent, NL2SQL, Mixed Recommendation
- Middleware: MySQL 8.0, Redis 7.x, RocketMQ 5.3, MongoDB 7.x
- Frontend: Vue 3, Element Plus, Pinia, Vite, uni-app

## 核心业务模块

### 交易域（trade）
- 购物车 / 订单：Redisson 分布式锁防重 + RocketMQ 延迟消息超时取消
- 支付中台：渠道策略模式（`IPayChannelHandler`），支持余额 / mock / 微信三渠道；微信渠道开发环境走 mock 模式，由 `/api/mock/wechat/pay-confirm` 模拟微信回调完成入账闭环
- WebSocket 通知：RocketMQ 广播消费 + 本机连接过滤，`/ws/{userId}` 定向推送

### 营销域（market）
- 秒杀：Redis Lua 原子扣库存 + 限购 → MQ 异步落库 → 延迟消息超时回补库存
- 优惠券 / 签到

### 社交域（social）
- 笔记 / 评价 / 关注
- 点赞：Redis 攒批 + 热点聚合 + 定时对账（DB 基线预热）

### AI 服务（savory-ai）
- ReAct Agent / NL2SQL 智能问数 / 混合推荐

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.9+
- Node.js 22+
- Docker Desktop（统一承载 MySQL / Redis / RocketMQ / PostgreSQL+pgvector / MongoDB）

### 1. 启动中间件
```bash
docker compose up -d
```

### 2. 初始化数据库
```bash
docker exec -i savory-mysql mysql -uroot -proot123 < db/init_all.sql
```

### 3. 启动主应用
```bash
cd savory-modules
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
```

### 4. 启动 AI 服务
```bash
cd savory-ai
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
```

### 5. 启动前端
```bash
cd savory-admin && npm run dev      # 管理端 :5173
cd savory-merchant && npm run dev   # 商家端 :5174
cd savory-screen && npm run dev     # 数据大屏 :5175
```

### 访问接口文档
http://localhost:8080/swagger-ui/index.html

主应用接口文档：http://localhost:8080/swagger-ui/index.html
AI 服务健康：http://localhost:8087/actuator/health
