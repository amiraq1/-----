package com.nabd.browser.downloads

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * مستقبل اكتمال التنزيلات
 */
class DownloadCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            
            if (downloadId != -1L) {
                // يتم التعامل مع هذا في DownloadHelper
                // هذا المستقبل موجود للتوافق مع الإصدارات القديمة
            }
        }
    }
}
