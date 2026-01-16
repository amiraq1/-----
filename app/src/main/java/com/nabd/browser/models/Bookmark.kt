package com.nabd.browser.models

import java.util.UUID

/**
 * نموذج المفضلة
 * يمثل صفحة محفوظة في المفضلة
 */
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROOT_FOLDER = "root"
        
        /**
         * إنشاء مفضلة من تبويب
         */
        fun fromTab(tab: Tab): Bookmark {
            return Bookmark(
                url = tab.url,
                title = tab.title,
                favicon = tab.favicon
            )
        }
    }
    
    /**
     * الحصول على اسم النطاق
     */
    val domain: String
        get() = try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
}

/**
 * نموذج مجلد المفضلة
 */
data class BookmarkFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
