package com.rednote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rednote.common.CursorResult;
import com.rednote.common.UserContext;
import com.rednote.entity.Comment;
import com.rednote.entity.CommentLike;
import com.rednote.entity.User;
import com.rednote.entity.dto.AddCommentDTO;
import com.rednote.entity.vo.CommentVO;
import com.rednote.entity.vo.PostDetailVO;
import com.rednote.mapper.CommentLikeMapper;
import com.rednote.mapper.CommentMapper;
import com.rednote.service.CommentService;
import com.rednote.service.PostService;
import com.rednote.service.UserService;
import com.rednote.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private UserService userService;

    @Resource
    private PostService postService;

    @Resource
    private AliOssUtil aliOssUtil;

    @Override
    public List<Comment> getRootComments(Long postId) {
        LambdaQueryWrapper<Comment> query = new LambdaQueryWrapper<>();
        query.eq(Comment::getPostId, postId)
                .isNull(Comment::getRootParentId) // 关键：只查一级评论
                .orderByDesc(Comment::getLikeCount); // 热门评论排前面
        return list(query);
    }

    @Override
    public CursorResult<CommentVO> getReplies(Long rootCommentId, String cursor, int size) {
        // 1. 解析游标
        Long lastId = null;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                lastId = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                // 忽略错误游标
            }
        }

        LambdaQueryWrapper<Comment> query = new LambdaQueryWrapper<>();
        query.eq(Comment::getRootParentId, rootCommentId);

        // 排序：时间正序，ID正序
        query.orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId);

        // 游标条件
        if (lastId != null) {
            query.gt(Comment::getId, lastId);
            query.last("LIMIT " + (size + 1));
        } else {
            // 如果没有游标（第一页），我们需要跳过第一条（因为第一条是 Top Reply，已经在 Feed 中展示了）
            // 所以我们需要查 size + 2 条：1条跳过 + size条数据 + 1条判断 hasMore
            query.last("LIMIT " + (size + 2));
        }

        List<Comment> comments = list(query);

        // 处理第一页跳过第一条的逻辑
        if (lastId == null && !comments.isEmpty()) {
            comments.remove(0);
        }

        // 处理 hasMore 和 nextCursor
        boolean hasMore = false;
        String nextCursor = null;
        if (comments.size() > size) {
            hasMore = true;
            comments.remove(comments.size() - 1);
            Comment last = comments.get(comments.size() - 1);
            nextCursor = String.valueOf(last.getId());
        } else if (!comments.isEmpty()) {
            Comment last = comments.get(comments.size() - 1);
            nextCursor = String.valueOf(last.getId());
        }

        if (comments.isEmpty()) {
            return CursorResult.build(new ArrayList<>(), nextCursor, hasMore);
        }

        List<CommentVO> voList = new ArrayList<>();
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());

        // 统一查询用户
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Long currentUserId = UserContext.getUserId();

        for (Comment comment : comments) {
            CommentVO vo = convertToVO(comment);
            fillUserInfo(vo, userMap);
            fillLikeStatus(vo, currentUserId);
            voList.add(vo);
        }
        return CursorResult.build(voList, nextCursor, hasMore);
    }

    @Override
    public CursorResult<CommentVO> getCommentFeed(Long postId, String cursor,
            int size) {
        // 1. 解析游标
        Integer lastLikeCount = null;
        Long lastId = null;
        if (cursor != null && !cursor.isEmpty()) {
            String[] parts = cursor.split("-");
            if (parts.length == 2) {
                lastLikeCount = Integer.parseInt(parts[0]);
                lastId = Long.parseLong(parts[1]);
            }
        }

        // 2. 联表查询 (包含用户信息和点赞状态)
        Long currentUserId = UserContext.getUserId();
        List<CommentVO> voList = baseMapper.selectCommentFeed(postId, lastLikeCount, lastId, size + 1, currentUserId);

        // 3. 处理游标
        boolean hasMore = false;
        String nextCursor = null;
        if (voList.size() > size) {
            hasMore = true;
            voList.remove(voList.size() - 1);
            CommentVO last = voList.get(voList.size() - 1);
            nextCursor = last.getLikeCount() + "-" + last.getId();
        } else if (!voList.isEmpty()) {
            CommentVO last = voList.get(voList.size() - 1);
            nextCursor = last.getLikeCount() + "-" + last.getId();
        }

        // 4. 填充 Top Reply 和 Reply Count
        if (!voList.isEmpty()) {
            List<Long> rootIds = voList.stream().map(CommentVO::getId).collect(Collectors.toList());

            // 获取帖子作者ID
            PostDetailVO postDetail = postService.getPostDetailById(postId);
            Long postAuthorId = postDetail != null ? postDetail.getAuthorId() : null;

            Map<Long, Comment> topReplyMap = new HashMap<>();
            Map<Long, Long> replyCountMap = new HashMap<>();
            Set<Long> topReplyUserIds = new java.util.HashSet<>();

            if (!rootIds.isEmpty()) {
                List<Comment> topReplies = baseMapper.selectTopReplies(rootIds, postAuthorId);
                for (Comment reply : topReplies) {
                    topReplyMap.put(reply.getRootParentId(), reply);
                    topReplyUserIds.add(reply.getUserId());
                }

                List<Map<String, Object>> counts = baseMapper.selectReplyCounts(rootIds);
                for (Map<String, Object> map : counts) {
                    Long rootId = ((Number) map.get("rootId")).longValue();
                    Long count = ((Number) map.get("count")).longValue();
                    replyCountMap.put(rootId, count);
                }
            }

            // 批量查询 Top Reply 的用户信息
            Map<Long, User> userMap = new HashMap<>();
            if (!topReplyUserIds.isEmpty()) {
                userMap = userService.listByIds(topReplyUserIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
            }

            for (CommentVO vo : voList) {
                // 填充 Reply Count
                vo.setReplyCount(replyCountMap.getOrDefault(vo.getId(), 0L));

                // 填充 Top Reply
                Comment topReply = topReplyMap.get(vo.getId());
                if (topReply != null) {
                    CommentVO topReplyVO = convertToVO(topReply);
                    // 填充 Top Reply 的用户信息
                    fillUserInfo(topReplyVO, userMap);
                    // 填充 Top Reply 的点赞状态
                    fillLikeStatus(topReplyVO, currentUserId);
                    vo.setTopReply(topReplyVO);
                }
            }
        }

        return CursorResult.build(voList, nextCursor, hasMore);
    }

    @Override
    public boolean likeComment(Long commentId, boolean isLike) {
        Long userId = UserContext.getUserId();
        if (isLike) {
            // 点赞
            LambdaQueryWrapper<CommentLike> query = new LambdaQueryWrapper<>();
            query.eq(CommentLike::getCommentId, commentId)
                    .eq(CommentLike::getUserId, userId);
            if (commentLikeMapper.selectCount(query) > 0) {
                return true;
            }

            CommentLike commentLike = new CommentLike();
            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(commentLike);

            baseMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, commentId)
                    .setSql("like_count = like_count + 1"));
        } else {
            // 取消点赞
            LambdaQueryWrapper<CommentLike> query = new LambdaQueryWrapper<>();
            query.eq(CommentLike::getCommentId, commentId)
                    .eq(CommentLike::getUserId, userId);
            int deleted = commentLikeMapper.delete(query);

            if (deleted > 0) {
                baseMapper.update(null,
                        new LambdaUpdateWrapper<Comment>()
                                .eq(Comment::getId, commentId)
                                .setSql("like_count = like_count - 1"));
            }
        }
        return true;
    }

    @Override
    public CommentVO publishComment(AddCommentDTO addCommentDTO, MultipartFile file) {
        Comment comment = new Comment();
        comment.setPostId(addCommentDTO.getPostId());
        comment.setContent(addCommentDTO.getContent());
        comment.setParentId(addCommentDTO.getParentId());

        // 自动推断 rootParentId，防止前端传错导致子评论统计失效
        if (addCommentDTO.getParentId() != null) {
            Comment parent = getById(addCommentDTO.getParentId());
            if (parent == null) {
                throw new RuntimeException("回复的父评论不存在");
            }
            if (parent.getRootParentId() != null) {
                // 父评论已经是楼中楼，跟随父评论的 rootId
                comment.setRootParentId(parent.getRootParentId());
            } else {
                // 父评论是一级评论，rootId 就是父评论 ID
                comment.setRootParentId(parent.getId());
            }
        } else {
            comment.setRootParentId(null);
        }

        comment.setReplyToUserId(addCommentDTO.getReplyToUserId());
        comment.setUserId(UserContext.getUserId());
        comment.setLikeCount(0);
        comment.setCreatedAt(LocalDateTime.now());

        // 如果DTO中传了宽高，先设置
        if (addCommentDTO.getImageWidth() != null) {
            comment.setImageWidth(addCommentDTO.getImageWidth());
        }
        if (addCommentDTO.getImageHeight() != null) {
            comment.setImageHeight(addCommentDTO.getImageHeight());
        }

        if (file != null && !file.isEmpty()) {
            try {
                String url = aliOssUtil.upload(file);
                comment.setImageUrl(url);

                // 获取图片宽高
                if (comment.getImageWidth() == null || comment.getImageHeight() == null || comment.getImageWidth() == 0
                        || comment.getImageHeight() == 0) {
                    try {
                        BufferedImage image = ImageIO.read(file.getInputStream());
                        if (image != null) {
                            comment.setImageWidth(image.getWidth());
                            comment.setImageHeight(image.getHeight());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("图片上传失败", e);
            }
        }
        save(comment);

        // 转换为VO并填充用户信息
        CommentVO vo = convertToVO(comment);
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
        }
        vo.setIsLiked(false); // 新评论肯定没点赞

        return vo;
    }

    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setImageUrl(comment.getImageUrl());
        vo.setImageWidth(comment.getImageWidth());
        vo.setImageHeight(comment.getImageHeight());
        return vo;
    }

    private void fillUserInfo(CommentVO vo,
            Map<Long, User> userMap) {
        User user = userMap.get(vo.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
        }
    }

    private void fillLikeStatus(CommentVO vo, Long currentUserId) {
        // 查询是否点赞
        LambdaQueryWrapper<CommentLike> query = new LambdaQueryWrapper<>();
        query.eq(CommentLike::getCommentId, vo.getId())
                .eq(CommentLike::getUserId, currentUserId);
        vo.setIsLiked(commentLikeMapper.selectCount(query) > 0);
    }
}