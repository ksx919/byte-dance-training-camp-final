package com.rednote.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PostDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "rednote_posts.db"
        const val DATABASE_VERSION = 1

        const val TABLE_PENDING_POSTS = "pending_posts"
        
        const val COLUMN_LOCAL_ID = "local_id"
        const val COLUMN_SERVER_ID = "server_id"
        const val COLUMN_STATUS = "status"
        const val COLUMN_PROGRESS = "progress"
        const val COLUMN_ERROR_MSG = "error_msg"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_IMAGES = "images" // JSON array string
        const val COLUMN_CREATE_TIME = "create_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_PENDING_POSTS (
                $COLUMN_LOCAL_ID TEXT PRIMARY KEY,
                $COLUMN_SERVER_ID INTEGER,
                $COLUMN_STATUS INTEGER,
                $COLUMN_PROGRESS INTEGER,
                $COLUMN_ERROR_MSG TEXT,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_IMAGES TEXT,
                $COLUMN_CREATE_TIME INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_POSTS")
        onCreate(db)
    }
}
