package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "comment_likes")
public class CommentLike {
    private Long id;

    private Long commentId;

    private Long userId;

    private LocalDateTime createdAt;
}
