package com.nabd.browser.data.db.dao

import androidx.room.*
import com.nabd.browser.data.db.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للسجل
 */
@Dao
interface HistoryDao {
    
    /**
     * الحصول على جميع السجل
     */
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    /**
     * الحصول على عنصر سجل بواسطة ID
     */
    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getHistoryById(id: String): HistoryEntity?
    
    /**
     * الحصول على عنصر سجل بواسطة URL
     */
    @Query("SELECT * FROM history WHERE url = :url ORDER BY visitedAt DESC LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?
    
    /**
     * البحث في السجل
     */
    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    
    /**
     * الحصول على سجل اليوم
     */
    @Query("SELECT * FROM history WHERE visitedAt >= :startOfDay ORDER BY visitedAt DESC")
    fun getTodayHistory(startOfDay: Long): Flow<List<HistoryEntity>>
    
    /**
     * الحصول على سجل الأسبوع
     */
    @Query("SELECT * FROM history WHERE visitedAt >= :startOfWeek ORDER BY visitedAt DESC")
    fun getWeekHistory(startOfWeek: Long): Flow<List<HistoryEntity>>
    
    /**
     * الحصول على آخر زيارات
     */
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryEntity>>
    
    /**
     * إضافة عنصر للسجل
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)
    
    /**
     * تحديث عدد الزيارات
     */
    @Query("UPDATE history SET visitCount = visitCount + 1, visitedAt = :visitedAt WHERE url = :url")
    suspend fun incrementVisitCount(url: String, visitedAt: Long = System.currentTimeMillis())
    
    /**
     * حذف عنصر من السجل
     */
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)
    
    /**
     * حذف عنصر بواسطة ID
     */
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)
    
    /**
     * حذف السجل الأقدم من تاريخ معين
     */
    @Query("DELETE FROM history WHERE visitedAt < :timestamp")
    suspend fun deleteHistoryOlderThan(timestamp: Long)
    
    /**
     * حذف جميع السجل
     */
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
    
    /**
     * عدد عناصر السجل
     */
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int
    
    /**
     * الحصول على أكثر المواقع زيارة
     */
    @Query("SELECT * FROM history ORDER BY visitCount DESC LIMIT :limit")
    fun getMostVisited(limit: Int = 10): Flow<List<HistoryEntity>>
}
