package com.rednote.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.rednote.data.api.RetrofitClient
import com.rednote.data.db.PostDbHelper
import com.rednote.data.model.post.PendingPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import androidx.core.net.toUri

object PostUploadManager {

    private const val TAG = "PostUploadManager"
    private var dbHelper: PostDbHelper? = null
    private var appContext: Context? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dbLock = ReentrantLock()

    private inline fun <T> withDbLock(block: () -> T): T = dbLock.withLock(block)

    // 用于 UI 观察的待发布帖子列表
    private val _pendingPosts = MutableStateFlow<List<PendingPost>>(emptyList())
    val pendingPosts: StateFlow<List<PendingPost>> = _pendingPosts.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            dbHelper = PostDbHelper(context.applicationContext)
            cleanupOldPosts() // 启动时清理旧记录
            refreshPendingPosts() // 初始化时加载数据
        }
    }

    /**
     * 清理旧的帖子记录（成功或失败的），只保留正在上传的
     */
    private fun cleanupOldPosts() = withDbLock {
        val db = dbHelper?.writableDatabase ?: return@withDbLock
        try {
            val whereClause = "${PostDbHelper.COLUMN_STATUS} IN (?, ?)"
            val whereArgs = arrayOf(
                PendingPost.STATUS_SUCCESS.toString(),
                PendingPost.STATUS_FAILED.toString()
            )
            db.delete(PostDbHelper.TABLE_PENDING_POSTS, whereClause, whereArgs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old posts", e)
        }
    }

    /**
     * 发布帖子
     */
    fun publish(context: Context, title: String, content: String, images: List<Uri>) {
        init(context) // 确保初始化

        scope.launch {
            val localId = UUID.randomUUID().toString()
            val imageStrings = images.map { it.toString() }
            
            val pendingPost = PendingPost(
                localId = localId,
                status = PendingPost.STATUS_PENDING,
                title = title,
                content = content,
                imageUris = imageStrings
            )

            // 1. 保存到数据库
            saveToDb(pendingPost)
            
            // 2. 刷新 Flow
            refreshPendingPosts()

            // 3. 启动 Worker
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString("localId", localId)
                .build()

            val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(uploadWorkRequest)
            Log.d(TAG, "Upload work enqueued for localId: $localId")
        }
    }

    private fun saveToDb(post: PendingPost) = withDbLock {
        val db = dbHelper?.writableDatabase ?: return@withDbLock
        val values = ContentValues().apply {
            put(PostDbHelper.COLUMN_LOCAL_ID, post.localId)
            put(PostDbHelper.COLUMN_STATUS, post.status)
            put(PostDbHelper.COLUMN_TITLE, post.title)
            put(PostDbHelper.COLUMN_CONTENT, post.content)
            put(PostDbHelper.COLUMN_IMAGES, gson.toJson(post.imageUris))
            put(PostDbHelper.COLUMN_CREATE_TIME, post.createTime)
            put(PostDbHelper.COLUMN_PROGRESS, post.progress)
        }
        db.insertWithOnConflict(PostDbHelper.TABLE_PENDING_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun updatePostStatus(localId: String, status: Int, serverId: Long? = null, errorMsg: String? = null) = withDbLock {
        val db = dbHelper?.writableDatabase ?: return@withDbLock
        val values = ContentValues().apply {
            put(PostDbHelper.COLUMN_STATUS, status)
            if (serverId != null) put(PostDbHelper.COLUMN_SERVER_ID, serverId)
            if (errorMsg != null) put(PostDbHelper.COLUMN_ERROR_MSG, errorMsg)
        }
        db.update(PostDbHelper.TABLE_PENDING_POSTS, values, "${PostDbHelper.COLUMN_LOCAL_ID} = ?", arrayOf(localId))
        refreshPendingPostsLocked()
    }
    
    private fun getPostFromDb(localId: String): PendingPost? = withDbLock {
        val db = dbHelper?.readableDatabase ?: return@withDbLock null
        val cursor = db.query(
            PostDbHelper.TABLE_PENDING_POSTS, null,
            "${PostDbHelper.COLUMN_LOCAL_ID} = ?", arrayOf(localId),
            null, null, null
        )
        return@withDbLock cursor.use {
            if (it.moveToFirst()) parseCursor(it) else null
        }
    }

    private fun refreshPendingPosts() = withDbLock {
        refreshPendingPostsLocked()
    }

    private fun refreshPendingPostsLocked() {
        val db = dbHelper?.readableDatabase ?: return
        val cursor = db.query(
            PostDbHelper.TABLE_PENDING_POSTS, null, null, null, null, null, 
            "${PostDbHelper.COLUMN_CREATE_TIME} DESC"
        )
        val list = mutableListOf<PendingPost>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseCursor(it))
            }
        }
        _pendingPosts.value = list
    }

    private fun parseCursor(cursor: Cursor): PendingPost {
        val localId = cursor.getString(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_LOCAL_ID))
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_STATUS))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_TITLE))
        val content = cursor.getString(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_CONTENT))
        val imagesJson = cursor.getString(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_IMAGES))
        val createTime = cursor.getLong(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_CREATE_TIME))
        val serverId = if (cursor.isNull(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_SERVER_ID))) null 
                       else cursor.getLong(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_SERVER_ID))
        val errorMsg = cursor.getString(cursor.getColumnIndexOrThrow(PostDbHelper.COLUMN_ERROR_MSG))

        val imageUris = try {
            val jsonArray = gson.fromJson(imagesJson, com.google.gson.JsonArray::class.java)
            val list = mutableListOf<String>()
            jsonArray.forEach { list.add(it.asString) }
            list
        } catch (_: Exception) {
            emptyList()
        }

        return PendingPost(localId, serverId, status, 0, errorMsg, title, content, imageUris, createTime)
    }

    /**
     * Worker 用于执行真正的上传任务
     */
    class UploadWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            val localId = inputData.getString("localId") ?: return@withContext Result.failure()
            
            // 确保 Manager 已初始化
            init(applicationContext)

            try {
                // 1. 获取任务详情
                val post = getPostFromDb(localId)
                if (post == null) {
                    Log.e(TAG, "Post not found: $localId")
                    return@withContext Result.failure()
                }

                // 2. 更新状态为上传中
                updatePostStatus(localId, PendingPost.STATUS_UPLOADING)

                // 3. 准备文件
                val imageUris = post.imageUris.map { it.toUri() }
                val files = imageUris.mapNotNull { uri ->
                    try {
                        uriToFile(applicationContext, uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to convert URI to file: $uri", e)
                        null
                    }
                }

                if (files.isEmpty()) {
                    updatePostStatus(localId, PendingPost.STATUS_FAILED, errorMsg = "图片文件丢失")
                    return@withContext Result.failure()
                }

                // 4. 获取第一张图片的宽高
                var width = 0
                var height = 0
                try {
                    val firstFile = files[0]
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(firstFile.absolutePath, options)
                    width = options.outWidth
                    height = options.outHeight
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode image bounds", e)
                }

                val fileParts = files.map { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files", file.name, requestFile)
                }

                // 5. 准备参数
                val titleBody = post.title.toRequestBody("text/plain".toMediaTypeOrNull())
                val contentBody = post.content.toRequestBody("text/plain".toMediaTypeOrNull())
                val widthBody = width.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val heightBody = height.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // 6. 调用 API
                val response = RetrofitClient.postApiService.publish(
                    title = titleBody,
                    content = contentBody,
                    imgWidth = widthBody,
                    imgHeight = heightBody,
                    files = fileParts
                )

                if (response.code == 200) {
                    val result = response.data
                    val serverId = result?.id
                    Log.d(TAG, "Upload success, serverId: $serverId")
                    
                    // 7. 更新状态为成功
                    updatePostStatus(localId, PendingPost.STATUS_SUCCESS, serverId = serverId)
                    
                    Result.success()
                } else {
                    val msg = response.msg ?: "Unknown error"
                    Log.e(TAG, "Upload failed: $msg")
                    updatePostStatus(localId, PendingPost.STATUS_FAILED, errorMsg = msg)
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                updatePostStatus(localId, PendingPost.STATUS_FAILED, errorMsg = e.message)
                Result.retry()
            }
        }

        private fun uriToFile(context: Context, uri: Uri): File? {
            return try {
                if (uri.scheme == "file") {
                    File(uri.path!!)
                } else {
                    val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
            } catch (e: Exception) {
                Log.e(TAG, "uriToFile error", e)
                null
            }
        }
    }
}
