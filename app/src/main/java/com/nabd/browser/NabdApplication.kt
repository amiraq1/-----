package com.nabd.browser

import android.app.Application
import com.nabd.browser.data.db.AppDatabase
import com.nabd.browser.data.repository.BrowserRepository

/**
 * تطبيق نبض الرئيسي
 * يُستخدم لتهيئة قاعدة البيانات والمكونات الأساسية
 */
class NabdApplication : Application() {
    
    /**
     * قاعدة البيانات
     */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    /**
     * Repository
     */
    val repository: BrowserRepository by lazy {
        BrowserRepository(
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao()
        )
    }
    
    companion object {
        private lateinit var instance: NabdApplication
        
        /**
         * الحصول على نسخة التطبيق
         */
        fun getInstance(): NabdApplication = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onTerminate() {
        super.onTerminate()
        AppDatabase.closeDatabase()
    }
}
