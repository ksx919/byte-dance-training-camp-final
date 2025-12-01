package com.rednote.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.rednote.data.db.DraftDbHelper
import com.rednote.data.model.Draft
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object DraftManager {

    private const val TAG = "DraftManager"
    private var dbHelper: DraftDbHelper? = null
    private var appContext: Context? = null
    private val gson = Gson()
    private val dbLock = ReentrantLock()

    private inline fun <T> withDbLock(block: () -> T): T = dbLock.withLock(block)

    @Synchronized
    fun init(context: Context) {
        // 只有未初始化时才赋值,防止重复覆盖
        if (appContext == null) {
            appContext = context.applicationContext
            dbHelper = DraftDbHelper(context.applicationContext)
        }
    }

    /**
     * 保存草稿(执行 IO 操作,建议在后台线程调用)
     * 【修复】不再复制图片,直接保存原始 URI
     * 这样可以在图片选择器中正确识别和反选草稿中的图片
     */
    fun saveDraft(title: String, content: String, originalImages: List<Uri>) {
        val context = appContext
        if (context == null) {
            Log.e(TAG, "saveDraft: Context is null! Please call DraftManager.init(this) in Application.")
            return
        }

        try {
            withDbLock {
                val db = dbHelper?.writableDatabase ?: return@withDbLock

                Log.d(TAG, "saveDraft: Starting save. Title: $title, Image count: ${originalImages.size}")
                originalImages.forEachIndexed { index, uri ->
                    Log.d(TAG, "saveDraft: Original image[$index]: $uri")
                }

                val imageStrings = originalImages.map { it.toString() }
                val imagesJson = gson.toJson(imageStrings)

                Log.d(TAG, "saveDraft: ImagesJson to save: $imagesJson")

                val values = ContentValues().apply {
                    put(DraftDbHelper.COLUMN_ID, 1)
                    put(DraftDbHelper.COLUMN_TITLE, title)
                    put(DraftDbHelper.COLUMN_CONTENT, content)
                    put(DraftDbHelper.COLUMN_IMAGES, imagesJson)
                    put(DraftDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
                }

                db.replace(DraftDbHelper.TABLE_DRAFTS, null, values)
                Log.d(TAG, "saveDraft: Success. Saved ${originalImages.size} images.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveDraft: Error occurred", e)
        }
    }

    fun getDraft(): Draft? {
        return try {
            withDbLock {
                val db = dbHelper?.readableDatabase ?: return@withDbLock null
                val cursor = db.query(
                    DraftDbHelper.TABLE_DRAFTS, null, "${DraftDbHelper.COLUMN_ID} = ?",
                    arrayOf("1"), null, null, null
                )
                cursor.use {
                    if (it.moveToFirst()) {
                        val title = it.getString(it.getColumnIndexOrThrow(DraftDbHelper.COLUMN_TITLE))
                        val content = it.getString(it.getColumnIndexOrThrow(DraftDbHelper.COLUMN_CONTENT))
                        val imagesJson = it.getString(it.getColumnIndexOrThrow(DraftDbHelper.COLUMN_IMAGES))
                        val updatedAt = it.getLong(it.getColumnIndexOrThrow(DraftDbHelper.COLUMN_UPDATED_AT))
                        Log.d(TAG, "getDraft: Found draft. Title: $title, ImagesJson: $imagesJson")
                        Draft(1, title, content, imagesJson, updatedAt)
                    } else {
                        Log.d(TAG, "getDraft: No draft found")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDraft: Error occurred", e)
            null
        }
    }

    fun parseImages(json: String): List<Uri> {
        return try {
            if (json.isBlank()) {
                Log.d(TAG, "parseImages: JSON is blank")
                return emptyList()
            }
            
            // 【修复】不使用 TypeToken,改用 JsonArray 避免 R8 混淆问题
            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            val strings = mutableListOf<String>()
            for (i in 0 until jsonArray.size()) {
                strings.add(jsonArray.get(i).asString)
            }
            
            // 将字符串重新解析为 Uri
            val uris = strings.map { Uri.parse(it) }
            Log.d(TAG, "parseImages: Parsed ${uris.size} URIs")
            uris.forEachIndexed { index, uri ->
                Log.d(TAG, "parseImages[$index]: $uri (scheme: ${uri.scheme})")
            }
            uris
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing images JSON", e)
            emptyList()
        }
    }

    fun clearDraft() {
        withDbLock {
            val db = dbHelper?.writableDatabase ?: return@withDbLock
            try {
                db.delete(DraftDbHelper.TABLE_DRAFTS, "${DraftDbHelper.COLUMN_ID} = ?", arrayOf("1"))
                Log.d(TAG, "Draft cleared.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hasDraft(): Boolean {
        return getDraft() != null
    }
}