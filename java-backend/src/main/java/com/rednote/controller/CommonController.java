package com.rednote.controller;

import com.rednote.common.Result;
import com.rednote.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传接口
     * 安卓端调用：POST /api/common/upload
     * Body: form-data, key="file", value=图片文件
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        try {
            String url = aliOssUtil.upload(file);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("文件上传失败");
        }
    }
}