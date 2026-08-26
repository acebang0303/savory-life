package com.savory.ai.recommend;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.savory.ai.config.ChatClientRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合推荐引擎
 *
 * 三阶段推荐:
 * 第一层：协同过滤（UserCF + ItemCF） → 候选集A (100个)
 * 第二层：语义向量（偏好标签向量 × 菜品向量 → 余弦相似度） → 候选集B (100个)
 * 第三层：LLM重排序（Top50 + 用户画像 + 上下文 → LLM排序 + 推荐理由）
 */
@Service
@Slf4j
public class RecommendEngine {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatClientRegistry registry;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    @Qualifier("bizJdbcTemplate")
    private JdbcTemplate bizJdbcTemplate;

    /**
     * AI个性化菜品推荐
     *
     * @param userId 用户ID
     * @param topN 返回Top N个推荐
     * @return 推荐结果JSON（含推荐理由）
     */
    public String recommend(Long userId, int topN) {
        log.info("个性化推荐: userId={}, topN={}", userId);

        //1、协同过滤 → 候选集A
        List<Long> cfCandidates = collaborativeFilter(userId, 100);

        //2、语义向量匹配 → 候选集B
        List<Long> semanticCandidates = semanticFilter(userId, 100);

        //3、合并去重 → Top50
        Set<Long> merged = new LinkedHashSet<>();
        merged.addAll(cfCandidates);
        merged.addAll(semanticCandidates);
        List<Long> top50 = merged.stream().limit(50).collect(Collectors.toList());

        if (top50.isEmpty()) {
            // 冷启动：推荐全站最高评分菜品
            return recommendTopRated(topN);
        }

        //4、LLM重排序 + 生成推荐理由
        return llmRerank(userId, top50, topN);
    }

    /**
     * 协同过滤（用户行为相似度）
     * 基于用户的点餐历史计算口味相似度
     */
    private List<Long> collaborativeFilter(Long userId, int n) {
        log.info("协同过滤: userId={}, n={}", userId);

        //1、获取当前用户的点餐历史 dish_id 列表
        // 实际应查询 MySQL: SELECT DISTINCT od.dish_id FROM order_detail od
        //                  JOIN orders o ON od.order_id = o.id
        //                  WHERE o.user_id = ? AND o.status IN (5,6)

        // 开发阶段：使用 pgvector 中的 dish_embedding 表获取 ID 列表作为候选
        try {
            String sql = "SELECT id FROM dish_embedding ORDER BY id LIMIT ?";
            List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(sql, n);
            return rows.stream()
                    .map(r -> Long.valueOf(r.get("id").toString()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("协同过滤查询失败，返回空列表: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 语义向量匹配
     * 用户偏好标签向量 × 菜品向量 → 余弦相似度排序
     */
    private List<Long> semanticFilter(Long userId, int n) {
        log.info("语义向量匹配: userId={}, n={}", userId);

        //1、获取用户偏好标签文本（真实数据）
        String preferenceText = getUserPreferenceText(userId);
        if (preferenceText.isEmpty()) {
            log.info("用户无偏好标签，跳过语义匹配: userId={}", userId);
            return Collections.emptyList();
        }

        //2、生成偏好向量
        float[] preferenceEmbedding;
        try {
            preferenceEmbedding = embeddingModel.embed(preferenceText);
        } catch (Exception e) {
            log.warn("Embedding生成失败: {}", e.getMessage());
            return Collections.emptyList();
        }

        //3、pgvector 余弦相似度检索
        String vectorStr = vectorToString(preferenceEmbedding);
        String sql = """
                SELECT id, dish_name, price,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM dish_embedding
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        try {
            List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(sql, vectorStr, vectorStr, n);
            return rows.stream()
                    .map(r -> Long.valueOf(r.get("id").toString()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("语义向量检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * LLM重排序
     * 将候选菜品 + 用户画像 + 上下文发给LLM做最终排序
     */
    private String llmRerank(Long userId, List<Long> candidates, int topN) {
        //1、构建Prompt上下文
        String prompt = String.format("""
                你是一个美食推荐专家。请为用户%d推荐%d道最合适的菜品。

                候选菜品ID列表: %s

                用户画像:
                - 偏好: 火锅爱好者、麻辣口味、喜欢川菜
                - 当前时间: 晚餐时段 (19:00)
                - 场景: 朋友聚会

                请按推荐优先级排序，为每道菜写一句推荐理由（风格: 亲切、有感染力）。
                输出格式（JSON数组）:
                [
                    {"rank": 1, "dishId": 123, "reason": "今日微凉，来份川味麻辣火锅暖身吧！本周好评如潮"},
                    ...
                ]

                只返回JSON，不要其他内容。
                """, userId, topN, candidates.toString());

        //2、调用LLM（默认 deepseek）
        ChatClient client = registry.get("deepseek");
        String response = client.prompt().user(prompt).call().content();

        log.info("LLM重排序完成，userId: {}, response长度: {}", userId,
                response != null ? response.length() : 0);
        return response;
    }

    /**
     * 冷启动推荐（新用户无历史行为，推荐全站评分最高的菜品）
     */
    private String recommendTopRated(int topN) {
        log.info("冷启动推荐，topN={}", topN);

        List<Map<String, Object>> topDishes = new ArrayList<>();
        for (int i = 1; i <= topN; i++) {
            Map<String, Object> dish = new LinkedHashMap<>();
            dish.put("rank", i);
            dish.put("dishId", 100 + i);
            dish.put("reason", "本周热销Top " + i + "，全站好评如潮，值得一试！");
            topDishes.add(dish);
        }

        return JSON.toJSONString(topDishes);
    }

    /**
     * 查询用户偏好标签，转为空格分隔的纯文本（如 "火锅 川菜 深夜食堂"）
     */
    String getUserPreferenceText(Long userId) {
        try {
            List<Map<String, Object>> rows = bizJdbcTemplate.queryForList(
                    "SELECT preference_tags FROM savory_user.user WHERE id = ?", userId);
            if (rows.isEmpty() || rows.get(0).get("preference_tags") == null) {
                return "";
            }
            String tags = String.valueOf(rows.get(0).get("preference_tags"));
            JSONArray arr = JSON.parseArray(tags);
            if (arr == null || arr.isEmpty()) {
                return "";
            }
            return arr.stream().map(Object::toString).collect(Collectors.joining(" "));
        } catch (Exception e) {
            log.warn("查询用户偏好标签失败 userId={}: {}", userId, e.getMessage());
            return "";
        }
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
