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
        Map<String, Object> row = new HashMap<>();
        row.put("preference_tags", "[\"火锅\",\"川菜\",\"深夜食堂\"]");
        when(bizJdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(List.of(row));

        String text = recommendEngine.getUserPreferenceText(1L);

        assertEquals("火锅 川菜 深夜食堂", text);
    }

    @Test
    void getUserPreferenceTextReturnsEmptyWhenMissing() {
        when(bizJdbcTemplate.queryForList(anyString(), eq(2L)))
                .thenReturn(Collections.emptyList());

        assertEquals("", recommendEngine.getUserPreferenceText(2L));
    }
}
