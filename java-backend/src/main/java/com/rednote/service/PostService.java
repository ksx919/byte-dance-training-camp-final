package com.rednote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rednote.common.CursorResult;
import com.rednote.entity.Post;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.entity.vo.PostDetailVO;
import org.springframework.web.multipart.MultipartFile;

public interface PostService extends IService<Post> {
    // 发布帖子
    PostDetailVO publishPost(PostPublishDTO postPublishDTO, MultipartFile[] files);

    // 用游标分页获取推荐流
    // lastId: 上一页最后一条的ID (可为空)
    // size: 每次加载多少条
    CursorResult<PostInfoVO> getFeedList(Long lastId, int size);

    PostDetailVO getPostDetailById(Long id);

    // 点赞/取消点赞
    boolean likePost(Long postId, boolean isLike);
}