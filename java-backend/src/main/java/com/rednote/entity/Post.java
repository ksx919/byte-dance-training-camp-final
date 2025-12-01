package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
// autoResultMap = true 必须开启，否则读取时无法自动把JSON转回List
@TableName(value = "posts", autoResultMap = true)
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    /**
     * 重点：图片集
     * 数据库里是 JSON 字符串 ["url1", "url2"]
     * Java里是 List<String>
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Integer likeCount;

    private Integer commentCount;

    private Integer imgWidth;

    private Integer imgHeight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}