package com.rednote.entity.vo;

import lombok.Data;

@Data
public class PostInfoVO {
    private Long id;

    private String title;

    private String nickname;

    private String avatarUrl;

    private Integer likeCount;

    private String image;

    private Integer height;

    private Integer width;

    private Boolean isLiked;
}
