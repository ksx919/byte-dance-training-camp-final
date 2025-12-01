package com.rednote.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;

    private String imageUrl;
    private Integer imageWidth;
    private Integer imageHeight;

    // 楼中楼回复 (仅显示一条)
    private CommentVO topReply;

    // 该评论下的总回复数
    private Long replyCount;
}
