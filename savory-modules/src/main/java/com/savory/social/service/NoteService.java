package com.savory.social.service;

import com.savory.pojo.entity.Note;
import com.savory.common.result.PageResult;

/**
 * 笔记服务接口
 * Feed流采用推拉结合模式
 */
public interface NoteService {

    /**
     * 发布笔记（推拉结合）
     * - 大V (>10000粉): 只写自己时间线，粉丝拉取
     * - 普通用户: 写入粉丝Feed收件箱（Redis ZSet）
     */
    void publish(Note note);

    /**
     * 首页推荐Feed流
     */
    PageResult feed(int page, int pageSize);

    /**
     * 热门笔记排行榜（Redis ZSet）
     * Score = likeCount*2 + commentCount*3 + collectCount*5
     */
    PageResult hotRanking(int page, int pageSize);

    /**
     * 点赞/取消赞
     */
    boolean like(Long noteId);

    /**
     * 收藏/取消收藏
     */
    boolean collect(Long noteId);
}
