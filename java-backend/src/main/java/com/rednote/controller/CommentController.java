package com.rednote.controller;

import com.rednote.common.CursorResult;
import com.rednote.common.Result;
import com.rednote.entity.Comment;
import com.rednote.entity.dto.AddCommentDTO;
import com.rednote.entity.vo.CommentVO;
import com.rednote.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    // 发表评论
    @PostMapping("/add")
    public Result<CommentVO> addComment(
            @RequestPart("comment") AddCommentDTO addCommentDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.success(commentService.publishComment(addCommentDTO, file));
    }

    // 获取一级评论列表
    @GetMapping("/roots")
    public Result<List<Comment>> getRoots(@RequestParam Long postId) {
        return Result.success(commentService.getRootComments(postId));
    }

    // 获取楼中楼回复
    @GetMapping("/replies")
    public Result<CursorResult<CommentVO>> getReplies(
            @RequestParam Long rootId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(commentService.getReplies(rootId, cursor, size));
    }

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result<Boolean> like(@RequestParam Long targetId, @RequestParam boolean isLike) {
        return Result.success(commentService.likeComment(targetId, isLike));
    }

    // 获取评论列表 (游标分页)
    @GetMapping("/feed")
    public Result<CursorResult<CommentVO>> getFeed(
            @RequestParam Long postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.getCommentFeed(postId, cursor, size));
    }
}