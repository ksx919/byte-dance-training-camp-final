package com.rednote.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private UserInfoVO userInfoVO;

    private String token;
}
