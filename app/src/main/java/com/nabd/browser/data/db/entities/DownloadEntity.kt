package com.nabd.browser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * حالات التنزيل
 */
enum class DownloadStatusDb {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * كيان التنزيل في قاعدة البيانات
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val filePath: String? = null,
    val mimeType: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = DownloadStatusDb.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val downloadManagerId: Long? = null
)
