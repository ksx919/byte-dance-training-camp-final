package com.rednote.controller;

import com.rednote.common.Result;
import com.rednote.common.UserContext;
import com.rednote.entity.User;
import com.rednote.entity.dto.LoginDTO;
import com.rednote.entity.dto.RegisterDTO;
import com.rednote.entity.vo.LoginVO;
import com.rednote.entity.vo.UserInfoVO;
import com.rednote.service.UserService;
import com.rednote.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 注册接口
     * 前端传入 JSON: { "email": "xx@xx.com", "passwordHash": "123456", "nickname": "小红" }
     */
    @PostMapping("/register")
    public Result<UserInfoVO> register(@RequestBody RegisterDTO registerDTO) {
        try {
            UserInfoVO newUser = userService.register(registerDTO.getEmail(),
                    registerDTO.getPassword(),
                    registerDTO.getNickname());
            return Result.success(newUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登录接口
     * 前端传入 JSON: { "email": "xx@xx.com", "password": "123456" }
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            UserInfoVO userInfoVO = userService.login(loginDTO.getEmail(), loginDTO.getPassword());

            String token = JwtUtils.generateToken(userInfoVO.getId());
            LoginVO loginVO = new LoginVO(userInfoVO, token);
            return Result.success(loginVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/get-info")
    public Result<User> getUserInfo() {
        Long id = UserContext.getUserId();
        return Result.success(userService.getById(id));
    }

    // 修改用户信息
    @PutMapping("/update")
    public Result<Boolean> updateUserInfo(@RequestBody User user) {
        return Result.success(userService.updateById(user));
    }

    //上传头像
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        Long id = UserContext.getUserId();
        return Result.success(userService.uploadAvatar(avatar, id));
    }
}
