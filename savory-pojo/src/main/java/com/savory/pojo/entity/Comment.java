package com.savory.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评论表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    //笔记ID
    private Long noteId;
    //评论者ID
    private Long userId;
    //父评论ID(支持二级回复)
    private Long parentId;
    //回复目标用户ID
    private Long replyToUserId;
    //评论内容
    private String content;
    //点赞数
    private Integer likeCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
