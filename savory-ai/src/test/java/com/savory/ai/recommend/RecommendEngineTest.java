package com.savory.ai.recommend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendEngineTest {

    @Mock(name = "bizJdbcTemplate")
    private JdbcTemplate bizJdbcTemplate;

    @InjectMocks
    private RecommendEngine recommendEngine;

    @Test
    void getUserPreferenceTextJoinsTags() {
        // 实现先查行为关联店铺名（双参 union SQL），再查已有偏好标签
        // 1) 店铺名查询返回空（无行为关联）
        when(bizJdbcTemplate.queryForList(anyString(), eq(1L), eq(1L)))
                .thenReturn(Collections.emptyList());
        // 2) 偏好标签查询返回该用户的 preference_tags
        Map<String, Object> row = new HashMap<>();
        row.put("preference_tags", "[\"火锅\",\"川菜\",\"深夜食堂\"]");
        when(bizJdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(List.of(row));

        String text = recommendEngine.getUserPreferenceText(1L);

        assertEquals("火锅 川菜 深夜食堂", text);
    }

    @Test
    void getUserPreferenceTextReturnsEmptyWhenMissing() {
        // 店铺名查询返回空 + 标签查询返回空行 → 结果为空字符串
        when(bizJdbcTemplate.queryForList(anyString(), eq(2L), eq(2L)))
                .thenReturn(Collections.emptyList());
        when(bizJdbcTemplate.queryForList(anyString(), eq(2L)))
                .thenReturn(Collections.emptyList());

        assertEquals("", recommendEngine.getUserPreferenceText(2L));
    }
}
