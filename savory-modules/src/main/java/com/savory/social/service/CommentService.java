package com.savory.social.service;

import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Comment;

/**
 * 评论服务接口
 * 支持二级回复（parent_id为null=一级评论, 非null=回复）
 */
public interface CommentService {

    /**
     * 发表评论
     */
    void publish(Comment comment);

    /**
     * 分页查询笔记的评论列表（含二级回复）
     */
    PageResult pageByNoteId(Long noteId, int page, int pageSize);

    /**
     * 删除评论
     */
    void deleteById(Long commentId);
}
