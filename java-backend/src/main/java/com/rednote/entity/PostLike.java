package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "post_likes")
public class PostLike {
    private Long id;

    private Long postId;

    private Long userId;

    private LocalDateTime createdAt;
}
