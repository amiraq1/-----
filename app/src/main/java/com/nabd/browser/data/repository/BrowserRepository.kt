package com.nabd.browser.data.repository

import com.nabd.browser.data.db.dao.BookmarkDao
import com.nabd.browser.data.db.dao.DownloadDao
import com.nabd.browser.data.db.dao.HistoryDao
import com.nabd.browser.data.db.entities.BookmarkEntity
import com.nabd.browser.data.db.entities.DownloadEntity
import com.nabd.browser.data.db.entities.HistoryEntity
import com.nabd.browser.models.Bookmark
import com.nabd.browser.models.DownloadItem
import com.nabd.browser.models.DownloadStatus
import com.nabd.browser.models.HistoryItem
import com.nabd.browser.models.Tab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository للمتصفح
 * يوفر طبقة وسيطة بين ViewModel وقاعدة البيانات
 */
class BrowserRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao
) {
    
    // ========================================
    // المفضلة
    // ========================================
    
    /**
     * الحصول على جميع المفضلات
     */
    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toBookmark() }
        }
    }
    
    /**
     * البحث في المفضلات
     */
    fun searchBookmarks(query: String): Flow<List<Bookmark>> {
        return bookmarkDao.searchBookmarks(query).map { entities ->
            entities.map { it.toBookmark() }
        }
    }
    
    /**
     * التحقق من وجود URL في المفضلة
     */
    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }
    
    /**
     * إضافة مفضلة
     */
    suspend fun addBookmark(tab: Tab) {
        val entity = BookmarkEntity(
            id = UUID.randomUUID().toString(),
            url = tab.url,
            title = tab.title,
            favicon = tab.favicon
        )
        bookmarkDao.insertBookmark(entity)
    }
    
    /**
     * إضافة مفضلة من Bookmark model
     */
    suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark.toEntity())
    }
    
    /**
     * حذف مفضلة بواسطة URL
     */
    suspend fun removeBookmark(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }
    
    /**
     * حذف مفضلة بواسطة ID
     */
    suspend fun removeBookmarkById(id: String) {
        bookmarkDao.deleteBookmarkById(id)
    }
    
    /**
     * حذف جميع المفضلات
     */
    suspend fun clearAllBookmarks() {
        bookmarkDao.deleteAllBookmarks()
    }
    
    // ========================================
    // السجل
    // ========================================
    
    /**
     * الحصول على جميع السجل
     */
    fun getAllHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    /**
     * الحصول على آخر الزيارات
     */
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryItem>> {
        return historyDao.getRecentHistory(limit).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    /**
     * البحث في السجل
     */
    fun searchHistory(query: String): Flow<List<HistoryItem>> {
        return historyDao.searchHistory(query).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    /**
     * الحصول على أكثر المواقع زيارة
     */
    fun getMostVisitedSites(limit: Int = 10): Flow<List<HistoryItem>> {
        return historyDao.getMostVisited(limit).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    /**
     * إضافة عنصر للسجل
     */
    suspend fun addToHistory(tab: Tab) {
        val existingHistory = historyDao.getHistoryByUrl(tab.url)
        if (existingHistory != null) {
            historyDao.incrementVisitCount(tab.url)
        } else {
            val entity = HistoryEntity(
                id = UUID.randomUUID().toString(),
                url = tab.url,
                title = tab.title,
                favicon = tab.favicon
            )
            historyDao.insertHistory(entity)
        }
    }
    
    /**
     * حذف عنصر من السجل
     */
    suspend fun removeHistoryItem(id: String) {
        historyDao.deleteHistoryById(id)
    }
    
    /**
     * حذف جميع السجل
     */
    suspend fun clearAllHistory() {
        historyDao.deleteAllHistory()
    }
    
    /**
     * حذف السجل الأقدم من عدد أيام معين
     */
    suspend fun clearHistoryOlderThan(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        historyDao.deleteHistoryOlderThan(cutoffTime)
    }
    
    // ========================================
    // التنزيلات
    // ========================================
    
    /**
     * الحصول على جميع التنزيلات
     */
    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }
    
    /**
     * الحصول على التنزيلات النشطة
     */
    fun getActiveDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getActiveDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }
    
    /**
     * إضافة تنزيل جديد
     */
    suspend fun addDownload(
        url: String,
        fileName: String,
        mimeType: String?,
        downloadManagerId: Long? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val entity = DownloadEntity(
            id = id,
            url = url,
            fileName = fileName,
            mimeType = mimeType,
            downloadManagerId = downloadManagerId
        )
        downloadDao.insertDownload(entity)
        return id
    }
    
    /**
     * تحديث حالة التنزيل
     */
    suspend fun updateDownloadStatus(id: String, status: DownloadStatus) {
        downloadDao.updateDownloadStatus(id, status.name)
    }
    
    /**
     * تحديث تقدم التنزيل
     */
    suspend fun updateDownloadProgress(id: String, downloadedBytes: Long, totalBytes: Long) {
        downloadDao.updateDownloadProgress(id, downloadedBytes, totalBytes)
    }
    
    /**
     * وضع علامة اكتمال التنزيل
     */
    suspend fun markDownloadCompleted(id: String, filePath: String?) {
        downloadDao.markDownloadCompleted(id, filePath = filePath)
    }
    
    /**
     * وضع علامة فشل التنزيل
     */
    suspend fun markDownloadFailed(id: String, errorMessage: String?) {
        downloadDao.markDownloadFailed(id, errorMessage)
    }
    
    /**
     * الحصول على تنزيل بواسطة Download Manager ID
     */
    suspend fun getDownloadByManagerId(downloadManagerId: Long): DownloadItem? {
        return downloadDao.getDownloadByManagerId(downloadManagerId)?.toDownloadItem()
    }
    
    /**
     * حذف تنزيل
     */
    suspend fun removeDownload(id: String) {
        downloadDao.deleteDownloadById(id)
    }
    
    /**
     * حذف جميع التنزيلات
     */
    suspend fun clearAllDownloads() {
        downloadDao.deleteAllDownloads()
    }
    
    // ========================================
    // تحويلات المساعدة
    // ========================================
    
    private fun BookmarkEntity.toBookmark(): Bookmark {
        return Bookmark(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            folderId = folderId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun Bookmark.toEntity(): BookmarkEntity {
        return BookmarkEntity(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            folderId = folderId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun HistoryEntity.toHistoryItem(): HistoryItem {
        return HistoryItem(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            visitedAt = visitedAt,
            visitCount = visitCount
        )
    }
    
    private fun DownloadEntity.toDownloadItem(): DownloadItem {
        return DownloadItem(
            id = id,
            url = url,
            fileName = fileName,
            filePath = filePath,
            mimeType = mimeType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.valueOf(status),
            createdAt = createdAt,
            completedAt = completedAt,
            errorMessage = errorMessage
        )
    }
}
