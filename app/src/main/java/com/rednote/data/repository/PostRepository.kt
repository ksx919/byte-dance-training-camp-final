package com.rednote.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.BaseResponse
import com.rednote.data.model.post.PostPublishDTO
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

object PostRepository {

    private val apiService = RetrofitClient.postApiService
    private val gson = Gson()

    suspend fun publish(
        context: Context,
        title: String,
        content: String,
        imageUris: List<Uri>
    ): BaseResponse<Boolean> {
        val dto = PostPublishDTO(title, content)
        val json = gson.toJson(dto)
        val dtoRequestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val parts = imageUris.mapNotNull { uri ->
            val file = getFileFromUri(context, uri)
            file?.let {
                val requestBody = it.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", it.name, requestBody)
            }
        }

        return apiService.publish(dtoRequestBody, parts)
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        // Simple implementation, can be improved to query OpenableColumns
        return uri.lastPathSegment
    }
}
