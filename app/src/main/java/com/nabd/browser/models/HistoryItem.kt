package com.nabd.browser.models

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نموذج عنصر السجل
 * يمثل صفحة تمت زيارتها
 */
data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val favicon: String? = null,
    val visitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
) {
    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
        private val timeFormat = SimpleDateFormat("HH:mm", Locale("ar"))
        
        /**
         * إنشاء عنصر سجل من تبويب
         */
        fun fromTab(tab: Tab): HistoryItem {
            return HistoryItem(
                url = tab.url,
                title = tab.title,
                favicon = tab.favicon
            )
        }
        
        /**
         * تجميع عناصر السجل حسب التاريخ
         */
        fun groupByDate(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
            return items.groupBy { item ->
                dateFormat.format(Date(item.visitedAt))
            }
        }
    }
    
    /**
     * الحصول على تاريخ الزيارة منسقًا
     */
    val formattedDate: String
        get() = dateFormat.format(Date(visitedAt))
    
    /**
     * الحصول على وقت الزيارة منسقًا
     */
    val formattedTime: String
        get() = timeFormat.format(Date(visitedAt))
    
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
    
    /**
     * التحقق مما إذا كانت الزيارة اليوم
     */
    val isToday: Boolean
        get() {
            val today = dateFormat.format(Date())
            return formattedDate == today
        }
    
    /**
     * الحصول على نص العرض للتاريخ
     */
    val displayDate: String
        get() = when {
            isToday -> "اليوم"
            isYesterday -> "أمس"
            else -> formattedDate
        }
    
    private val isYesterday: Boolean
        get() {
            val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            return formattedDate == dateFormat.format(yesterday)
        }
}
