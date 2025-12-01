package com.rednote.entity.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String email;

    private String password;

    private String nickname;
}
