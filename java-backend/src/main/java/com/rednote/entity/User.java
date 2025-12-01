package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users") // 对应数据库表名
public class User {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String email;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String bio;

    // 自动填充创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 自动填充更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}