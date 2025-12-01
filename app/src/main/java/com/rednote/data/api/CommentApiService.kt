package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.comment.CommentVO
import com.rednote.data.model.post.CursorResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface CommentApiService {

    @GET("comments/feed")
    suspend fun getFeed(
        @Query("postId") postId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int = 10
    ): BaseResponse<CursorResult<CommentVO>>

    @GET("comments/replies")
    suspend fun getReplies(
        @Query("rootId") rootId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int = 5
    ): BaseResponse<CursorResult<CommentVO>>

    @Multipart
    @POST("comments/add")
    suspend fun addComment(
        @Part("comment") comment: RequestBody,
        @Part file: MultipartBody.Part?
    ): BaseResponse<CommentVO>

    @POST("comments/like")
    suspend fun like(
        @Query("targetId") targetId: Long,
        @Query("isLike") isLike: Boolean
    ): BaseResponse<Boolean>
}
