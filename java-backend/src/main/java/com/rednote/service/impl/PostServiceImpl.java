package com.rednote.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rednote.common.CursorResult;
import com.rednote.common.UserContext;
import com.rednote.entity.Post;
import com.rednote.entity.User;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.vo.PostDetailVO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.entity.vo.PostUploadVO;
import com.rednote.mapper.PostMapper;
import com.rednote.service.PostService;
import com.rednote.service.UserService;
import com.rednote.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private UserService userService;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private com.rednote.mapper.PostLikeMapper postLikeMapper;

    @Override
    public PostDetailVO publishPost(PostPublishDTO postPublishDTO, MultipartFile[] files) {
        Post post = new Post();
        post.setTitle(postPublishDTO.getTitle());
        post.setContent(postPublishDTO.getContent());
        post.setUserId(UserContext.getUserId());
        post.setImages(aliOssUtil.uploadImages(files));
        post.setImgHeight(postPublishDTO.getImgHeight());
        post.setImgWidth(postPublishDTO.getImgWidth());
        if (!save(post)) {
            throw new RuntimeException("发布失败");
        }
        return BeanUtil.copyProperties(post, PostDetailVO.class);
    }

    @Override
    public CursorResult<PostInfoVO> getFeedList(Long lastId, int size) {
        // 1. 构造查询条件
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();

        // 核心逻辑：ID 倒序 (最新的在上面)
        query.orderByDesc(Post::getId);

        // 如果传了 lastId，说明是加载更多，要查 ID 比这个小的
        // SQL: WHERE id < {lastId}
        if (lastId != null) {
            query.lt(Post::getId, lastId);
        }

        // 限制条数：为了判断“是否还有更多”，我们故意多查 1 条
        query.last("LIMIT " + (size + 1));

        // 2. 执行查询
        List<Post> posts = list(query);

        // 3. 处理游标和 hasMore
        boolean hasMore = false;
        Long nextCursor = null;

        if (posts.size() > size) {
            hasMore = true;
            // 把多查的那一条删掉，不返回给前端
            posts.remove(posts.size() - 1);
            // 下一次的游标，就是当前列表最后一条的 ID
            nextCursor = posts.get(posts.size() - 1).getId();
        } else if (!posts.isEmpty()) {
            nextCursor = posts.get(posts.size() - 1).getId();
        }

        // 4. 转换为 PostInfoVO 并填充用户信息
        List<PostInfoVO> voList = new ArrayList<>();
        if (!posts.isEmpty()) {
            // 收集所有 userId
            Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
            // 批量查询用户
            List<User> users = userService.listByIds(userIds);
            // 转为 Map 方便查找
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

            // 批量查询当前用户是否点赞
            Long currentUserId = UserContext.getUserId();
            Set<Long> likedPostIds = new java.util.HashSet<>();
            if (currentUserId != null) {
                List<Long> currentBatchPostIds = posts.stream().map(Post::getId).collect(Collectors.toList());
                if (!currentBatchPostIds.isEmpty()) {
                    LambdaQueryWrapper<com.rednote.entity.PostLike> likeQuery = new LambdaQueryWrapper<>();
                    likeQuery.eq(com.rednote.entity.PostLike::getUserId, currentUserId)
                            .in(com.rednote.entity.PostLike::getPostId, currentBatchPostIds);
                    List<com.rednote.entity.PostLike> likes = postLikeMapper.selectList(likeQuery);
                    likedPostIds = likes.stream().map(com.rednote.entity.PostLike::getPostId)
                            .collect(Collectors.toSet());
                }
            }

            for (Post post : posts) {
                PostInfoVO vo = new PostInfoVO();
                vo.setId(post.getId());
                vo.setTitle(post.getTitle());
                vo.setLikeCount(post.getLikeCount());
                vo.setIsLiked(likedPostIds.contains(post.getId()));

                // 设置第一张图片
                List<String> images = post.getImages();
                if (images != null && !images.isEmpty()) {
                    vo.setImage(images.getFirst());
                }

                // 设置宽高
                vo.setWidth(post.getImgWidth());
                vo.setHeight(post.getImgHeight());

                // 设置用户信息
                User user = userMap.get(post.getUserId());
                if (user != null) {
                    vo.setNickname(user.getNickname());
                    vo.setAvatarUrl(user.getAvatarUrl());
                }

                voList.add(vo);
            }
        }

        return CursorResult.build(voList, nextCursor != null ? String.valueOf(nextCursor) : null, hasMore);
    }

    @Override
    public PostDetailVO getPostDetailById(Long id) {
        Long currentUserId = UserContext.getUserId();
        return baseMapper.selectPostDetail(id, currentUserId);
    }

    @Override
    public boolean likePost(Long postId, boolean isLike) {
        Long userId = UserContext.getUserId();
        if (isLike) {
            // 点赞
            // 1. 检查是否已经点赞
            LambdaQueryWrapper<com.rednote.entity.PostLike> query = new LambdaQueryWrapper<>();
            query.eq(com.rednote.entity.PostLike::getPostId, postId)
                    .eq(com.rednote.entity.PostLike::getUserId, userId);
            if (postLikeMapper.selectCount(query) > 0) {
                return true; // 已经点赞过了
            }

            // 2. 插入点赞记录
            com.rednote.entity.PostLike postLike = new com.rednote.entity.PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(userId);
            postLike.setCreatedAt(java.time.LocalDateTime.now());
            postLikeMapper.insert(postLike);

            // 3. 更新帖子点赞数
            // update posts set like_count = like_count + 1 where id = postId
            baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("like_count = like_count + 1"));
        } else {
            // 取消点赞
            // 1. 删除点赞记录
            LambdaQueryWrapper<com.rednote.entity.PostLike> query = new LambdaQueryWrapper<>();
            query.eq(com.rednote.entity.PostLike::getPostId, postId)
                    .eq(com.rednote.entity.PostLike::getUserId, userId);
            int deleted = postLikeMapper.delete(query);

            if (deleted > 0) {
                // 2. 更新帖子点赞数
                baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("like_count = like_count - 1"));
            }
        }
        return true;
    }
}