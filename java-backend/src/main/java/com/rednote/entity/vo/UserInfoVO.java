package com.rednote.entity.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
}
