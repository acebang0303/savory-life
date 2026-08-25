package com.savory.social.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.context.BaseContext;
import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Note;
import com.savory.pojo.entity.NoteLike;
import com.savory.social.mapper.NoteLikeMapper;
import com.savory.social.mapper.NoteMapper;
import com.savory.social.mq.NoteEmbeddingProducer;
import com.savory.social.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 笔记服务实现类
 *
 * Feed流策略:
 * - 大V(粉丝>10000): 只写自己时间线，粉丝拉模式实时拉取
 * - 普通用户: 写粉丝Feed收件箱(Redis Sorted Set, 上限500条)
 *
 * 热门排行榜:
 * - Redis ZSet: hot_notes:weekly
 * - Score = likeCount*2 + commentCount*3 + collectCount*5
 */
@Service
@DS("social")
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteLikeMapper noteLikeMapper;

    @Autowired
    private NoteEmbeddingProducer noteEmbeddingProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String HOT_NOTES_KEY = "hot_notes:weekly";
    private static final int FEED_MAX_SIZE = 500;

    @Override
    @Transactional
    public void publish(Note note) {
        //1、设置作者ID
        note.setUserId(BaseContext.getCurrentId());
        note.setAuditStatus(0); //待审核

        //2、保存笔记
        noteMapper.insert(note);
        log.info("笔记发布成功，noteId: {}, userId: {}", note.getId(), note.getUserId());

        //发消息同步笔记向量（消费端按 audit_status 决定是否重建）
        noteEmbeddingProducer.send(note.getId());

        //3、异步触发AI内容审核
        //TODO: RocketMQ消息 → AI Audit Agent → 更新audit_status

        //4、笔记写入Feed流
        // 普通用户的粉丝Feed收件箱由消费者异步写入
        // TODO: 判断粉丝数量，决定推模式还是拉模式
    }

    @Override
    public PageResult feed(int page, int pageSize) {
        Long userId = BaseContext.getCurrentId();

        //1、拉模式: 从关注的大V获取最新笔记
        //TODO: 查询关注列表 → 拉取大V的最新笔记

        //2、推模式: 从Redis收件箱获取
        String feedKey = "feed:" + userId;
        Set<ZSetOperations.TypedTuple<Object>> feedItems = redisTemplate.opsForZSet()
                .reverseRangeWithScores(feedKey, 0, pageSize - 1);

        //3、合并排序 → 分页返回
        if (feedItems == null || feedItems.isEmpty()) {
            // 降级: 查全站最新笔记
            Page<Note> p = new Page<>(page, pageSize);
            LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Note::getAuditStatus, 1) // 已审核通过
                   .orderByDesc(Note::getCreateTime);
            Page<Note> result = noteMapper.selectPage(p, wrapper);
            return new PageResult(result.getTotal(), result.getRecords());
        }

        //TODO: 解析Feed内容，批量查询笔记详情
        return new PageResult(0, null);
    }

    @Override
    public PageResult hotRanking(int page, int pageSize) {
        //1、从Redis ZSet获取热门排行（按score倒序）
        Set<ZSetOperations.TypedTuple<Object>> hotNotes = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_NOTES_KEY,
                        (long) (page - 1) * pageSize,
                        (long) page * pageSize - 1);

        //2、提取笔记ID列表
        //TODO: 批量查询笔记详情

        //3、获取总数量
        Long total = redisTemplate.opsForZSet().size(HOT_NOTES_KEY);
        return new PageResult(total != null ? total : 0, null);
    }

    @Override
    @Transactional
    public boolean like(Long noteId) {
        Long userId = BaseContext.getCurrentId();

        //1、检查是否已点赞
        LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteLike::getNoteId, noteId)
               .eq(NoteLike::getUserId, userId);
        NoteLike exist = noteLikeMapper.selectOne(wrapper);

        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return false;
        }

        if (exist != null) {
            //2、已点赞 → 取消赞
            noteLikeMapper.deleteById(exist.getId());
            note.setLikeCount(Math.max(0, note.getLikeCount() - 1));
            noteMapper.updateById(note);

            //更新热度分
            updateHotScore(noteId, note);
            return false;
        } else {
            //3、未点赞 → 点赞
            NoteLike noteLike = NoteLike.builder()
                    .noteId(noteId)
                    .userId(userId)
                    .build();
            noteLikeMapper.insert(noteLike);

            note.setLikeCount((note.getLikeCount() != null ? note.getLikeCount() : 0) + 1);
            noteMapper.updateById(note);

            //更新热度分
            updateHotScore(noteId, note);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean collect(Long noteId) {
        Long userId = BaseContext.getCurrentId();
        String collectKey = "note:collect:" + noteId;

        //1、Redis Set存储收藏关系
        Boolean isCollected = redisTemplate.opsForSet().isMember(collectKey, userId.toString());
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return false;
        }

        if (Boolean.TRUE.equals(isCollected)) {
            //2、取消收藏
            redisTemplate.opsForSet().remove(collectKey, userId.toString());
            note.setCollectCount(Math.max(0, note.getCollectCount() - 1));
            noteMapper.updateById(note);
            return false;
        } else {
            //3、收藏
            redisTemplate.opsForSet().add(collectKey, userId.toString());
            note.setCollectCount((note.getCollectCount() != null ? note.getCollectCount() : 0) + 1);
            noteMapper.updateById(note);
            return true;
        }
    }

    /**
     * 更新笔记热度分
     * Score = likeCount*2 + commentCount*3 + collectCount*5
     */
    private void updateHotScore(Long noteId, Note note) {
        double score = (note.getLikeCount() != null ? note.getLikeCount() : 0) * 2.0
                + (note.getCommentCount() != null ? note.getCommentCount() : 0) * 3.0
                + (note.getCollectCount() != null ? note.getCollectCount() : 0) * 5.0;
        redisTemplate.opsForZSet().add(HOT_NOTES_KEY, noteId.toString(), score);
    }
}
