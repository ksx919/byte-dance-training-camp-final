package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    private Integer likeCount;

    // 父评论ID (如果是直接回复帖子，则为null)
    private Long parentId;

    // 根评论ID (用于楼中楼快速查找)
    private Long rootParentId;

    // 被回复的用户ID
    private Long replyToUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private String imageUrl;

    private Integer imageWidth;

    private Integer imageHeight;
}