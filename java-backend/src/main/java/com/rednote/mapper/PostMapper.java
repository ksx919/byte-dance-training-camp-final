package com.rednote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rednote.entity.Post;
import com.rednote.entity.vo.PostDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    PostDetailVO selectPostDetail(@Param("postId") Long postId, @Param("userId") Long userId);
}