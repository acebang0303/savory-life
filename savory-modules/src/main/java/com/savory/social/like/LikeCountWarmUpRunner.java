package com.savory.social.like;

import com.savory.pojo.entity.Note;
import com.savory.social.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 点赞计数预热：以 DB note.like_count 为权威基线写入 Redis note:like:count。
 * SyncReconciler 以 Redis 为权威整刷 DB，若无基线则 Redis 从 0 累计的增量会被当作全量，
 * 覆盖掉 DB 里含历史 seed 的全量计数（点赞后被对账改成 1，取消后变 0）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LikeCountWarmUpRunner implements ApplicationRunner {

    private final NoteMapper noteMapper;
    private final NoteLikeStore store;

    @Override
    public void run(ApplicationArguments args) {
        List<Note> notes = noteMapper.selectList(null);
        int loaded = 0;
        for (Note n : notes) {
            if (n.getId() == null || n.getLikeCount() == null) {
                continue;
            }
            store.overwriteCount(n.getId(), n.getLikeCount());
            loaded++;
        }
        log.info("点赞计数预热完成: 从 DB 加载 {} 篇笔记的 like_count 到 Redis", loaded);
    }
}
