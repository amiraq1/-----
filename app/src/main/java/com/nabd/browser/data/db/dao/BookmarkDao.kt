package com.nabd.browser.data.db.dao

import androidx.room.*
import com.nabd.browser.data.db.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للمفضلة
 */
@Dao
interface BookmarkDao {
    
    /**
     * الحصول على جميع المفضلات
     */
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    
    /**
     * الحصول على مفضلة بواسطة ID
     */
    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: String): BookmarkEntity?
    
    /**
     * الحصول على مفضلة بواسطة URL
     */
    @Query("SELECT * FROM bookmarks WHERE url = :url")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?
    
    /**
     * التحقق من وجود URL في المفضلة
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean
    
    /**
     * البحث في المفضلات
     */
    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>
    
    /**
     * الحصول على المفضلات في مجلد معين
     */
    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getBookmarksByFolder(folderId: String?): Flow<List<BookmarkEntity>>
    
    /**
     * إضافة مفضلة
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)
    
    /**
     * إضافة مفضلات متعددة
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)
    
    /**
     * تحديث مفضلة
     */
    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)
    
    /**
     * حذف مفضلة
     */
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
    
    /**
     * حذف مفضلة بواسطة ID
     */
    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)
    
    /**
     * حذف مفضلة بواسطة URL
     */
    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)
    
    /**
     * حذف جميع المفضلات
     */
    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
    
    /**
     * عدد المفضلات
     */
    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarksCount(): Int
}
