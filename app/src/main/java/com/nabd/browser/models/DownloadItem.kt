package com.nabd.browser.models

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حالات التنزيل
 */
enum class DownloadStatus {
    PENDING,      // قيد الانتظار
    DOWNLOADING,  // جاري التنزيل
    PAUSED,       // متوقف مؤقتًا
    COMPLETED,    // مكتمل
    FAILED,       // فشل
    CANCELLED     // ملغي
}

/**
 * نموذج عنصر التنزيل
 */
data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val filePath: String? = null,
    val mimeType: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
) {
    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        
        /**
         * تحديد نوع الملف من MIME type
         */
        fun getFileType(mimeType: String?): FileType {
            return when {
                mimeType == null -> FileType.OTHER
                mimeType.startsWith("image/") -> FileType.IMAGE
                mimeType.startsWith("video/") -> FileType.VIDEO
                mimeType.startsWith("audio/") -> FileType.AUDIO
                mimeType.startsWith("application/pdf") -> FileType.PDF
                mimeType.contains("zip") || mimeType.contains("rar") || 
                mimeType.contains("tar") || mimeType.contains("7z") -> FileType.ARCHIVE
                mimeType.startsWith("text/") -> FileType.TEXT
                mimeType.contains("document") || mimeType.contains("word") -> FileType.DOCUMENT
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> FileType.SPREADSHEET
                mimeType.contains("presentation") || mimeType.contains("powerpoint") -> FileType.PRESENTATION
                mimeType.startsWith("application/") -> FileType.APPLICATION
                else -> FileType.OTHER
            }
        }
    }
    
    /**
     * نسبة التقدم (0-100)
     */
    val progress: Int
        get() = if (totalBytes > 0) {
            ((downloadedBytes.toFloat() / totalBytes) * 100).toInt()
        } else 0
    
    /**
     * هل التنزيل نشط
     */
    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING
    
    /**
     * هل يمكن استئناف التنزيل
     */
    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED
    
    /**
     * حجم الملف منسقًا
     */
    val formattedSize: String
        get() = formatBytes(totalBytes)
    
    /**
     * الحجم المُنزَّل منسقًا
     */
    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)
    
    /**
     * تاريخ الإنشاء منسقًا
     */
    val formattedDate: String
        get() = dateFormat.format(Date(createdAt))
    
    /**
     * نوع الملف
     */
    val fileType: FileType
        get() = getFileType(mimeType)
    
    /**
     * تنسيق حجم الملف
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}

/**
 * أنواع الملفات
 */
enum class FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    ARCHIVE,
    TEXT,
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    APPLICATION,
    OTHER
}
