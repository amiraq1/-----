package com.nabd.browser.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nabd.browser.data.db.dao.BookmarkDao
import com.nabd.browser.data.db.dao.DownloadDao
import com.nabd.browser.data.db.dao.HistoryDao
import com.nabd.browser.data.db.entities.BookmarkEntity
import com.nabd.browser.data.db.entities.DownloadEntity
import com.nabd.browser.data.db.entities.HistoryEntity

/**
 * قاعدة بيانات Room للمتصفح
 * تحتوي على جميع الجداول: المفضلة، السجل، التنزيلات
 */
@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * DAO للمفضلة
     */
    abstract fun bookmarkDao(): BookmarkDao
    
    /**
     * DAO للسجل
     */
    abstract fun historyDao(): HistoryDao
    
    /**
     * DAO للتنزيلات
     */
    abstract fun downloadDao(): DownloadDao
    
    companion object {
        private const val DATABASE_NAME = "nabd_browser_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * الحصول على نسخة من قاعدة البيانات (Singleton)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * بناء قاعدة البيانات
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
        
        /**
         * إغلاق قاعدة البيانات
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
