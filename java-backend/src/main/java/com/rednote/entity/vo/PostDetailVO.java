package com.rednote.entity.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PostDetailVO {
    // === 帖子基础信息 ===
    private Long id; // 帖子ID，点赞/收藏接口需要用
    private String title;
    private String content;
    private List<String> images;
    private Integer imgWidth; // 核心：用于前端计算比例
    private Integer imgHeight; // 核心：用于前端计算比例
    private LocalDateTime createdAt;

    // === 作者信息 (必须包含，否则前端无法展示顶部栏) ===
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    // === 交互数据 ===
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isLiked; // 状态：当前登录用户是否点赞了该贴
}