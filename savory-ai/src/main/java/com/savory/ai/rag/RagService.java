package com.savory.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG检索服务
 * 混合检索: BM25关键词 + 语义向量 → RRF融合 → LLM重排序
 */
@Service
@Slf4j
public class RagService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    /**
     * 语义向量检索
     *
     * @param query 用户查询
     * @param knowledgeBase 知识库名称
     * @param topK 返回Top K个结果
     */
    public List<Map<String, Object>> semanticSearch(String query, String knowledgeBase, int topK) {
        //1、生成查询向量
        float[] queryEmbedding = embeddingModel.embed(query);

        //2、pgvector 余弦相似度检索
        String sql = """
                SELECT id, title, content, metadata,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM knowledge_document
                WHERE knowledge_base = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        String vectorStr = vectorToString(queryEmbedding);
        return pgJdbcTemplate.queryForList(sql, vectorStr, knowledgeBase, vectorStr, topK);
    }

    /**
     * 加载文档到知识库
     * 1. 文档切分为 Chunk (500字/块, 50字重叠)
     * 2. 对每个Chunk生成Embedding
     * 3. 写入pgvector
     */
    public void loadDocument(String knowledgeBase, String title, String content) {
        log.info("加载文档到知识库: base={}, title={}", knowledgeBase, title);

        //1、文档切块（简易实现，生产使用 OpenNLP/HanLP）
        List<String> chunks = chunkText(content, 500, 50);

        //2、逐块生成向量并写入
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingModel.embed(chunk);

            String sql = """
                    INSERT INTO knowledge_document
                        (knowledge_base, title, content, chunk_index, embedding)
                    VALUES (?, ?, ?, ?, ?::vector)
                    """;

            pgJdbcTemplate.update(sql, knowledgeBase, title, chunk, i,
                    vectorToString(embedding));
        }

        log.info("文档加载完成: {}个chunk", chunks.size());
    }

    /**
     * 文本切块
     * 简单按字符数切分，尽量在标点符号处切分，最大回溯50字符防死循环
     */
    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int len = text.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            // 在标点符号处切分，但最多回溯50个字符防止死循环
            int scanEnd = end;
            for (int j = end - 1; j > start && j > end - 50; j--) {
                if (isChinesePunctuation(text.charAt(j))) {
                    scanEnd = j + 1;
                    break;
                }
            }
            end = scanEnd;
            chunks.add(text.substring(start, end));
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    private boolean isChinesePunctuation(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；' || c == '\n';
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
