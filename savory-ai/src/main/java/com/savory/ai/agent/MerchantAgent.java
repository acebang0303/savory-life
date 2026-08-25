package com.savory.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.savory.ai.config.AgentProperties;
import com.savory.ai.nlsql.SqlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商家经营助手Agent（NL2SQL模式）
 * 自然语言 → LLM生成SQL → 安全校验 → 执行真实业务库 → LLM总结成自然语言答案
 *
 * 安全机制:
 * 1. 只允许 SELECT 语句（SqlValidator 白名单/黑名单多层校验）
 * 2. 禁止 INSERT, UPDATE, DELETE, DROP, ALTER 等写操作
 * 3. 最大返回行数限制（无 LIMIT 时自动补 LIMIT 100）
 * 4. 校验失败 → 明确报错，不执行
 */
@Component
@Slf4j
public class MerchantAgent {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private SqlValidator sqlValidator;

    @Autowired
    @Qualifier("bizJdbcTemplate")
    private JdbcTemplate bizJdbcTemplate;

    private static final String NL2SQL_PROMPT = """
            你是一个SQL查询助手，负责将商家经营相关的问题转换为MySQL SELECT查询语句。

            数据库表结构（跨库查询，请使用全限定表名）：
            - savory_merchant.dish: 菜品表 (id, merchant_id, category_id, name, price, status, sales, create_time)，sales为累计销量
            - savory_trade.orders: 订单表 (id, number, user_id, merchant_id, amount, pay_amount, pay_status, status, create_time, pay_time)
            - savory_trade.order_detail: 订单明细 (id, order_id, name菜品名, amount, number数量)
            - savory_social.review: 评价表 (id, user_id, order_id, dish_id, rating评分, content内容, create_time)，无 merchant_id 字段，按商家过滤时需 JOIN savory_trade.orders ON review.order_id = orders.id
            - savory_user.user: 用户表 (id, nickname, phone, create_time)

            规则（按问题类型选择正确的数据源）：
            1. 只生成 SELECT 语句，不要有任何写操作
            2. 所有查询必须包含 merchant_id 过滤条件（dish.merchant_id 或 orders.merchant_id）
            3. 「销量」「卖得最好」「热销」「排行榜」类问题：直接用 savory_merchant.dish 表的 sales 字段 ORDER BY sales DESC，不要用订单明细统计
            4. 「营收」「流水」「订单数」「客单价」类问题：使用 savory_trade.orders 表，金额字段用 pay_amount
            5. 「评价」「差评」「好评」「评分」类问题：使用 savory_social.review 表
            6. 需要分页时使用 LIMIT offset, count 格式
            7. 日期过滤使用 DATE() 函数

            请只返回SQL语句，不要有任何解释。
            """;

    private static final String ANSWER_PROMPT = """
            你是餐饮经营数据分析助手。根据数据库查询结果，用自然、专业、简洁的中文回答商家的问题。

            要求：
            1. 直接给出结论和数据，不要提及SQL、表名、字段名
            2. 不要编造查询结果里没有的数据
            3. 如果数据不足以回答，如实说明
            4. 适当给出经营建议
            5. 用列表或分段使回答清晰
            """;

    /**
     * NL2SQL 对话（SSE流式输出）
     * 返回的 Flux 是「总结答案」的流；前置的 SQL 生成/校验/执行若失败，返回单元素错误 Flux
     */
    public Flux<String> execute(String question, Long empId) {
        log.info("商家经营Agent处理: empId={}, question={}", empId, question);

        //1、empId → merchantId
        Long merchantId = resolveMerchantId(empId);
        if (merchantId == null) {
            return Flux.just("未找到对应的商户信息，请联系管理员确认账号绑定。");
        }

        //2、LLM生成SQL
        String sql = generateSql(question, merchantId);
        if (sql == null || sql.isEmpty()) {
            return Flux.just("抱歉，我没能理解这个问题，请换个说法试试。");
        }

        //3、校验 + 执行
        if (!sqlValidator.validate(sql)) {
            return Flux.just("抱歉，该问题涉及的数据无法查询，请换一个经营相关的问题。");
        }
        sql = ensureLimit(sql);

        List<Map<String, Object>> rows;
        try {
            rows = bizJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.warn("经营助手SQL执行失败: sql={}, err={}", sql, e.getMessage());
            return Flux.just("查询失败：" + e.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return Flux.just("没有查询到相关数据。");
        }

        //4、LLM总结答案（流式）
        return summarize(question, rows);
    }

    private Long resolveMerchantId(Long empId) {
        try {
            List<Map<String, Object>> rows = bizJdbcTemplate.queryForList(
                    "SELECT id FROM savory_merchant.merchant_info WHERE emp_id = ?", empId);
            if (rows == null || rows.isEmpty()) {
                log.warn("未找到 emp_id={} 对应的商户", empId);
                return null;
            }
            return ((Number) rows.get(0).get("id")).longValue();
        } catch (Exception e) {
            log.warn("解析 merchantId 失败: empId={}, err={}", empId, e.getMessage());
            return null;
        }
    }

    private String generateSql(String question, Long merchantId) {
        String prompt = String.format("商家ID: %d\n问题: %s\n请生成SQL查询，所有查询必须限定 merchant_id = %d。",
                merchantId, question, merchantId);
        try {
            String content = ChatClient.builder(chatModel).build()
                    .prompt()
                    .system(NL2SQL_PROMPT)
                    .user(prompt)
                    .call()
                    .content();
            log.info("LLM生成SQL: {}", content);
            return extractSql(content);
        } catch (Exception e) {
            log.warn("LLM生成SQL失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractSql(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        // 剥离 markdown 代码块
        Matcher m = Pattern.compile("```(?:sql)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        // 否则取 SELECT 开头到行末/分号
        int idx = content.toUpperCase().indexOf("SELECT");
        if (idx < 0) {
            return content.trim();
        }
        String sql = content.substring(idx).trim();
        int semi = sql.indexOf(';');
        if (semi > 0) {
            sql = sql.substring(0, semi);
        }
        return sql.trim();
    }

    private String ensureLimit(String sql) {
        if (!sql.toUpperCase().contains("LIMIT")) {
            return sql + " LIMIT 100";
        }
        return sql;
    }

    private Flux<String> summarize(String question, List<Map<String, Object>> rows) {
        String dataJson = JSON.toJSONString(rows);
        String prompt = String.format("商家问题：%s\n查询结果：%s\n请基于以上查询结果回答商家的问题。",
                question, dataJson);
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(ANSWER_PROMPT)
                .user(prompt)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(agentProperties.getTimeoutSeconds()));
    }
}
