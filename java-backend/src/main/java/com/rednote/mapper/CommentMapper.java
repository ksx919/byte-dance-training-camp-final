package com.rednote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rednote.entity.Comment;
import com.rednote.entity.vo.CommentVO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
        /**
         * 批量查询一级评论下的最佳回复（Top Reply）
         * 优先级：时间最早
         */
        List<Comment> selectTopReplies(
                        @Param("rootIds") List<Long> rootIds,
                        @Param("postAuthorId") Long postAuthorId);

        /**
         * 批量统计一级评论下的回复数
         */
        @MapKey("rootId")
        List<Map<String, Object>> selectReplyCounts(
                        @Param("rootIds") List<Long> rootIds);

        /**
         * 联表查询评论列表
         */
        List<CommentVO> selectCommentFeed(
                        @Param("postId") Long postId,
                        @Param("lastLikeCount") Integer lastLikeCount,
                        @Param("lastId") Long lastId,
                        @Param("limit") int limit,
                        @Param("currentUserId") Long currentUserId);
}