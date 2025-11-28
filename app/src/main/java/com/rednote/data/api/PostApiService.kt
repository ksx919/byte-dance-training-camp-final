package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.post.CursorResult
import com.rednote.data.model.post.PostInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface PostApiService {

    @GET("posts/feed")
    suspend fun getFeed(
        @Query("lastId") lastId: Long? = null,
        @Query("size") size: Int = 4
    ): BaseResponse<CursorResult<PostInfo>>
}
