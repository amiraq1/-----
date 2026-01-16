package com.nabd.browser.data.db.dao

import androidx.room.*
import com.nabd.browser.data.db.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للتنزيلات
 */
@Dao
interface DownloadDao {
    
    /**
     * الحصول على جميع التنزيلات
     */
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    /**
     * الحصول على تنزيل بواسطة ID
     */
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?
    
    /**
     * الحصول على تنزيل بواسطة Download Manager ID
     */
    @Query("SELECT * FROM downloads WHERE downloadManagerId = :downloadManagerId")
    suspend fun getDownloadByManagerId(downloadManagerId: Long): DownloadEntity?
    
    /**
     * الحصول على التنزيلات النشطة
     */
    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>
    
    /**
     * الحصول على التنزيلات المكتملة
     */
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>
    
    /**
     * إضافة تنزيل
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)
    
    /**
     * تحديث تنزيل
     */
    @Update
    suspend fun updateDownload(download: DownloadEntity)
    
    /**
     * تحديث حالة التنزيل
     */
    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String)
    
    /**
     * تحديث تقدم التنزيل
     */
    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, downloadedBytes: Long, totalBytes: Long)
    
    /**
     * تحديث اكتمال التنزيل
     */
    @Query("UPDATE downloads SET status = 'COMPLETED', completedAt = :completedAt, filePath = :filePath WHERE id = :id")
    suspend fun markDownloadCompleted(id: String, completedAt: Long = System.currentTimeMillis(), filePath: String?)
    
    /**
     * تحديث فشل التنزيل
     */
    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markDownloadFailed(id: String, errorMessage: String?)
    
    /**
     * حذف تنزيل
     */
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    /**
     * حذف تنزيل بواسطة ID
     */
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)
    
    /**
     * حذف التنزيلات المكتملة
     */
    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedDownloads()
    
    /**
     * حذف جميع التنزيلات
     */
    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()
    
    /**
     * عدد التنزيلات
     */
    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun getDownloadsCount(): Int
    
    /**
     * عدد التنزيلات النشطة
     */
    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING')")
    suspend fun getActiveDownloadsCount(): Int
}
