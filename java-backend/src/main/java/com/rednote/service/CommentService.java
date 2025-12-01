package com.rednote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rednote.common.CursorResult;
import com.rednote.entity.Comment;
import com.rednote.entity.dto.AddCommentDTO;
import com.rednote.entity.vo.CommentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommentService extends IService<Comment> {
    // 获取某个帖子下的一级评论
    List<Comment> getRootComments(Long postId);

    // 获取某个一级评论下的所有回复 (展开更多回复)
    CursorResult<CommentVO> getReplies(Long rootCommentId, String cursor, int size);

    // 点赞/取消点赞
    boolean likeComment(Long commentId, boolean isLike);

    // 获取评论列表（游标分页）
    // cursor: "likeCount-id"
    CursorResult<CommentVO> getCommentFeed(Long postId, String cursor,
            int size);

    // 发表评论 (支持图片)
    CommentVO publishComment(AddCommentDTO addCommentDTO, MultipartFile file);
}