package com.rednote.controller;

import com.rednote.common.CursorResult;
import com.rednote.common.Result;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.vo.PostDetailVO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Resource
    private PostService postService;

    // 发布帖子
    @PostMapping("/publish")
    public Result<PostDetailVO> publish(
            PostPublishDTO postPublishDTO,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        return Result.success(postService.publishPost(postPublishDTO, files));
    }

    // 获取帖子详情
    @GetMapping("/{id}")
    public Result<PostDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(postService.getPostDetailById(id));
    }

    // 获取首页流 (Feed)
    @GetMapping("/feed")
    public Result<CursorResult<PostInfoVO>> getFeed(
            @RequestParam(required = false) Long lastId, // 第一次传 null
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.getFeedList(lastId, size));
    }

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result<Boolean> like(@RequestParam Long targetId, @RequestParam boolean isLike) {
        return Result.success(postService.likePost(targetId, isLike));
    }
}