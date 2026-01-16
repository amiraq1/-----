package com.nabd.browser.models

import java.util.UUID

/**
 * نموذج التبويب
 * يمثل تبويبًا واحدًا في المتصفح
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://www.google.com",
    val title: String = "تبويب جديد",
    val favicon: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_HOME_URL = "https://www.google.com"
        const val BLANK_PAGE = "about:blank"
        
        /**
         * إنشاء تبويب جديد
         */
        fun createNew(isIncognito: Boolean = false): Tab {
            return Tab(
                url = DEFAULT_HOME_URL,
                title = if (isIncognito) "تصفح خاص" else "تبويب جديد",
                isIncognito = isIncognito
            )
        }
    }
    
    /**
     * الحصول على اسم النطاق من الرابط
     */
    val domain: String
        get() = try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    
    /**
     * هل الرابط آمن (HTTPS)
     */
    val isSecure: Boolean
        get() = url.startsWith("https://")
}
