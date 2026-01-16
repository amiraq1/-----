package com.nabd.browser.downloads

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nabd.browser.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * مساعد التنزيلات
 * يتعامل مع تحميل الملفات باستخدام DownloadManager
 */
class DownloadHelper(
    private val context: Context,
    private val repository: BrowserRepository
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // خريطة لتتبع التنزيلات
    private val downloadIdMap = mutableMapOf<Long, String>()
    
    /**
     * BroadcastReceiver لتتبع اكتمال التنزيل
     */
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
            
            if (downloadId != -1L) {
                handleDownloadComplete(downloadId)
            }
        }
    }
    
    /**
     * تسجيل المستقبل
     */
    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }
    
    /**
     * إلغاء تسجيل المستقبل
     */
    fun unregister() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * بدء تنزيل ملف
     */
    fun startDownload(
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        onStarted: ((String) -> Unit)? = null
    ) {
        scope.launch {
            try {
                // استخراج اسم الملف
                val fileName = guessFileName(url, contentDisposition, mimeType)
                
                // إنشاء طلب التنزيل
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(fileName)
                    setDescription("جاري التنزيل...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                    
                    // إضافة headers إذا لزم الأمر
                    addRequestHeader("User-Agent", DEFAULT_USER_AGENT)
                    
                    if (mimeType != null) {
                        setMimeType(mimeType)
                    }
                }
                
                // بدء التنزيل
                val downloadManagerId = downloadManager.enqueue(request)
                
                // حفظ في قاعدة البيانات
                val internalId = repository.addDownload(
                    url = url,
                    fileName = fileName,
                    mimeType = mimeType,
                    downloadManagerId = downloadManagerId
                )
                
                // ربط المعرفات
                downloadIdMap[downloadManagerId] = internalId
                
                // إشعار المستخدم
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "بدأ تنزيل: $fileName", Toast.LENGTH_SHORT).show()
                    onStarted?.invoke(internalId)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "فشل بدء التنزيل: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * معالجة اكتمال التنزيل
     */
    private fun handleDownloadComplete(downloadManagerId: Long) {
        scope.launch {
            val query = DownloadManager.Query().setFilterById(downloadManagerId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)
                
                val internalId = downloadIdMap[downloadManagerId]
                    ?: repository.getDownloadByManagerId(downloadManagerId)?.id
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val localUri = cursor.getString(localUriIndex)
                        
                        internalId?.let {
                            repository.markDownloadCompleted(it, localUri)
                        }
                        
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "اكتمل التنزيل", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    DownloadManager.STATUS_FAILED -> {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        val errorMessage = getErrorMessage(reason)
                        
                        internalId?.let {
                            repository.markDownloadFailed(it, errorMessage)
                        }
                        
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "فشل التنزيل: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                // إزالة من الخريطة
                downloadIdMap.remove(downloadManagerId)
            }
            cursor.close()
        }
    }
    
    /**
     * فتح ملف منزل
     */
    fun openDownloadedFile(filePath: String, mimeType: String?) {
        try {
            val file = File(Uri.parse(filePath).path ?: return)
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "لا يمكن فتح الملف", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * إلغاء تنزيل
     */
    fun cancelDownload(downloadManagerId: Long) {
        downloadManager.remove(downloadManagerId)
        downloadIdMap.remove(downloadManagerId)
    }
    
    /**
     * تخمين اسم الملف
     */
    private fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }
    
    /**
     * الحصول على MIME type من مسار الملف
     */
    private fun getMimeType(filePath: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
    
    /**
     * الحصول على رسالة الخطأ
     */
    private fun getErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "لا يمكن استئناف التنزيل"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "جهاز التخزين غير موجود"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "الملف موجود بالفعل"
            DownloadManager.ERROR_FILE_ERROR -> "خطأ في الملف"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "خطأ في البيانات"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "مساحة غير كافية"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "عدد كبير من إعادة التوجيهات"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "خطأ HTTP غير معالج"
            DownloadManager.ERROR_UNKNOWN -> "خطأ غير معروف"
            else -> "خطأ: $reason"
        }
    }
    
    companion object {
        private const val DEFAULT_USER_AGENT = 
            "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
