package com.rednote.entity.dto;

import lombok.Data;

@Data
public class AddCommentDTO {
    private Long postId;
    private String content;
    private Long parentId;
    private Long rootParentId;
    private Long replyToUserId;
    private Integer imageWidth;
    private Integer imageHeight;
}
