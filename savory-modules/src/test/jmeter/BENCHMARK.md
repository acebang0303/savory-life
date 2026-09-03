# 秒杀压测基准报告

> 记录每次压测的容量数据。运行方式见 `run_load.sh`。前置：后端运行、`gen_tokens.sh` 生成 tokens.csv、重置活动库存。

## 环境
- 后端：savory-modules（事务消息版），localhost:8080
- MySQL/Redis/RocketMQ：Docker 容器
- JMeter：apache-jmeter-5.6.3
- 时间：YYYY-MM-DD

## 压测参数
- 活动 ID、菜品 ID、初始库存、并发数、ramp-up、线程数

## 结果

| 指标 | 值 |
|---|---|
| 总请求数 | |
| 成功数 (code=1) | |
| 失败数 (code=0) | |
| 失败分布（售罄/重复/限流/错误） | |
| TPS | |
| p50 / p90 / p95 延迟 (ms) | |

## 一致性校验（压测后）

| 数据源 | 值 | 判定 |
|---|---|---|
| Redis 库存 | | |
| DB stock | | |
| DB sold | | |
| 订单数 | | |
| 是否超卖（订单数 > 初始库存） | | |

## 结论与备注
- （记录瓶颈、异常、优化建议）
