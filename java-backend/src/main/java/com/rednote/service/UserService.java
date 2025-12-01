package com.rednote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rednote.entity.User;
import com.rednote.entity.vo.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
    // 定义业务接口：注册
    UserInfoVO register(String email, String password, String nickname);

    // 定义业务接口：根据邮箱登录
    UserInfoVO login(String email, String password);

    // 添加业务接口：上传头像
    String uploadAvatar(MultipartFile avatar,Long userId);
}