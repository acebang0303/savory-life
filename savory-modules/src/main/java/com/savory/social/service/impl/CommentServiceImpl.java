package com.savory.social.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.context.BaseContext;
import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Comment;
import com.savory.social.mapper.CommentMapper;
import com.savory.social.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 评论服务实现类
 * 一级评论：parent_id IS NULL
 * 二级回复：parent_id 指向一级评论ID
 */
@DS("social")
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public void publish(Comment comment) {
        //1、设置评论者ID
        comment.setUserId(BaseContext.getCurrentId());

        //2、保存评论
        commentMapper.insert(comment);
        log.info("评论发表成功，commentId: {}, noteId: {}, userId: {}",
                comment.getId(), comment.getNoteId(), comment.getUserId());
    }

    @Override
    public PageResult pageByNoteId(Long noteId, int page, int pageSize) {
        //1、先查一级评论（parent_id IS NULL）
        Page<Comment> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getNoteId, noteId)
               .isNull(Comment::getParentId)
               .orderByDesc(Comment::getCreateTime);

        //2、执行分页查询
        Page<Comment> result = commentMapper.selectPage(p, wrapper);

        //3、为每条一级评论查询其二级回复
        //TODO: 批量查询每个一级评论的回复列表
        return new PageResult(result.getTotal(), result.getRecords());
    }

    @Override
    public void deleteById(Long commentId) {
        //1、删除评论
        commentMapper.deleteById(commentId);
        log.info("评论删除，commentId: {}", commentId);
    }
}
