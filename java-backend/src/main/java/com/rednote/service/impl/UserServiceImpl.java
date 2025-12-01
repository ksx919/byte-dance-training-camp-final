package com.rednote.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rednote.entity.User;
import com.rednote.entity.vo.UserInfoVO;
import com.rednote.mapper.UserMapper;
import com.rednote.service.UserService;
import com.rednote.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private AliOssUtil aliOssUtil;

    @Override
    public UserInfoVO register(String email, String password, String nickname) {
        // 1. 检查邮箱是否已存在
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getEmail, email);
        if (count(query) > 0) {
            throw new RuntimeException("邮箱已被注册");
        }
        User user = new User();
        // 2. 密码加密
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setEmail(email);
        user.setNickname(nickname);

        // 3. 设置一些默认值
        if (nickname == null || nickname.isEmpty()) {
            user.setNickname("默认昵称"); // 默认昵称
        }

        // 4. 保存到数据库
        save(user);

        return BeanUtil.copyProperties(user, UserInfoVO.class);
    }

    @Override
    public UserInfoVO login(String email, String password) {
        // 1. 根据邮箱查询用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getEmail, email);
        User user = getOne(query);

        // 2. 校验用户是否存在
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 核心：校验密码
        boolean isMatch = BCrypt.checkpw(password, user.getPasswordHash());

        if (!isMatch) {
            throw new RuntimeException("密码错误");
        }

        // 使用hutool将User转为UserInfoVO
        return BeanUtil.copyProperties(user, UserInfoVO.class);
    }

    @Override
    public String uploadAvatar(MultipartFile avatar, Long userId) {
        String avatarUrl;
        // 1. 上传文件到 OSS
        avatarUrl = aliOssUtil.upload(avatar);

        // 2. 使用 MyBatis-Plus 更新数据库
        // update user set avatar = {avatarUrl} where id = {userId}
        boolean isSuccess = this.lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getAvatarUrl, avatarUrl)
                .update();

        if (!isSuccess) {
            throw new RuntimeException("更新用户头像失败");
        }

        return avatarUrl;
    }
}