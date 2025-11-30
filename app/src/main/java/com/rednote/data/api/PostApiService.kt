package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.post.CursorResult
import com.rednote.data.model.post.PostDetailVO
import com.rednote.data.model.post.PostInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PostApiService {

    @GET("posts/feed")
    suspend fun getFeed(
        @Query("lastId") lastId: Long? = null,
        @Query("size") size: Int = 4
    ): BaseResponse<CursorResult<PostInfo>>

    @Multipart
    @POST("posts/publish")
    suspend fun publish(
        @Part("postPublishDTO") postPublishDTO: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): BaseResponse<Boolean>

    @GET("posts/{id}")
    suspend fun getDetail(
        @Path("id") id: Long
    ): BaseResponse<PostDetailVO>

}
