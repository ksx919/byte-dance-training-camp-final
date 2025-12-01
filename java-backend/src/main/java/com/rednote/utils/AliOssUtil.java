package com.rednote.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.rednote.config.AliOssProperties;
import com.rednote.entity.vo.PostUploadVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class AliOssUtil {

    @Resource
    private AliOssProperties aliOssProperties;

    /**
     * 上传文件
     *
     * @param file 前端传来的文件对象
     * @return 文件的访问 URL
     */
    public String upload(MultipartFile file) {
        // 1. 获取配置信息
        String endpoint = aliOssProperties.getEndpoint();
        String accessKeyId = aliOssProperties.getAccessKeyId();
        String accessKeySecret = aliOssProperties.getAccessKeySecret();
        String bucketName = aliOssProperties.getBucketName();

        // 2. 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 3. 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 4. 生成文件名
            // 原始文件名：cat.jpg
            String originalFilename = file.getOriginalFilename();
            // 后缀：.jpg
            String extension = null;
            if (originalFilename != null) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // 新文件名：UUID + 后缀 -> d8s7-f6d8-s6d8.jpg
            String fileName = UUID.randomUUID() + extension;

            // 5. 优化存储目录：按日期分组 2025/11/26/d8s7...jpg
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String fullPath = datePath + "/" + fileName;

            // 6. 执行上传
            // PutObjectRequest 第一个参数是 BucketName，第二个参数是 OSS 中的完整路径
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fullPath, inputStream);
            ossClient.putObject(putObjectRequest);

            // 7. 拼接返回 URL (基于公共读权限)
            // 格式：https://{bucketName}.{endpoint}/{fullPath}
            return "https://" + bucketName + "." + endpoint + "/" + fullPath;

        } catch (Exception e) {
            throw new RuntimeException("头像上传失败");
        } finally {
            // 8. 关闭 OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 一次性上传多张图片
     *
     * @param files 前端传来的多个文件对象
     * @return 文件的访问 URL的列表
     */
    public List<String> uploadImages(MultipartFile[] files) {
        // 1. 获取配置信息
        String endpoint = aliOssProperties.getEndpoint();
        String accessKeyId = aliOssProperties.getAccessKeyId();
        String accessKeySecret = aliOssProperties.getAccessKeySecret();
        String bucketName = aliOssProperties.getBucketName();

        // 2. 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        List<String> urls = new java.util.ArrayList<>();

        try {
            // 5. 优化存储目录：按日期分组 2025/11/26
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

            for (MultipartFile file : files) {
                // 3. 获取文件输入流
                InputStream inputStream = file.getInputStream();

                // 4. 生成文件名
                // 原始文件名：cat.jpg
                String originalFilename = file.getOriginalFilename();
                // 后缀：.jpg
                String extension = ".jpg";
                if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                // 新文件名：UUID + 后缀 -> d8s7-f6d8-s6d8.jpg
                String fileName = UUID.randomUUID() + extension;

                String fullPath = datePath + "/" + fileName;

                // 6. 执行上传
                // PutObjectRequest 第一个参数是 BucketName，第二个参数是 OSS 中的完整路径
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fullPath, inputStream);
                ossClient.putObject(putObjectRequest);

                // 7. 拼接返回 URL (基于公共读权限)
                // 格式：https://{bucketName}.{endpoint}/{fullPath}
                String url = "https://" + bucketName + "." + endpoint + "/" + fullPath;
                urls.add(url);
            }
            return urls;

        } catch (Exception e) {
            throw new RuntimeException("图片上传失败");
        } finally {
            // 8. 关闭 OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}