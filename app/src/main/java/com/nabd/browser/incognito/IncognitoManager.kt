package com.nabd.browser.incognito

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * مدير التصفح الخاص (Incognito)
 * يدير إعدادات الخصوصية ومسح البيانات
 */
class IncognitoManager(private val context: Context) {
    
    private val cookieManager = CookieManager.getInstance()
    private val webStorage = WebStorage.getInstance()
    
    // قائمة WebViews في وضع التصفح الخاص
    private val incognitoWebViews = mutableListOf<WebView>()
    
    /**
     * تسجيل WebView كوضع خاص
     */
    fun registerIncognitoWebView(webView: WebView) {
        incognitoWebViews.add(webView)
        configureForIncognito(webView)
    }
    
    /**
     * إلغاء تسجيل WebView
     */
    fun unregisterIncognitoWebView(webView: WebView) {
        incognitoWebViews.remove(webView)
    }
    
    /**
     * تكوين WebView للوضع الخاص
     */
    private fun configureForIncognito(webView: WebView) {
        webView.settings.apply {
            // تعطيل حفظ كلمات المرور
            saveFormData = false
            
            // تعطيل التخزين المؤقت
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            
            // تعطيل قاعدة البيانات
            databaseEnabled = false
        }
        
        // مسح البيانات الموجودة لهذا WebView
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
    }
    
    /**
     * مسح جميع بيانات التصفح الخاص
     */
    fun clearIncognitoData() {
        // مسح cookies
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        
        // مسح WebStorage
        webStorage.deleteAllData()
        
        // مسح بيانات كل WebView خاص
        incognitoWebViews.forEach { webView ->
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
        }
    }
    
    /**
     * مسح cookies فقط
     */
    fun clearCookies() {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }
    
    /**
     * مسح التخزين المؤقت
     */
    fun clearCache() {
        incognitoWebViews.forEach { it.clearCache(true) }
    }
    
    /**
     * مسح بيانات النماذج
     */
    fun clearFormData() {
        incognitoWebViews.forEach { it.clearFormData() }
    }
    
    /**
     * مسح السجل (داخل WebView)
     */
    fun clearWebViewHistory() {
        incognitoWebViews.forEach { it.clearHistory() }
    }
    
    /**
     * إغلاق وتنظيف جميع WebViews الخاصة
     */
    fun closeAllIncognitoTabs() {
        incognitoWebViews.forEach { webView ->
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            webView.destroy()
        }
        incognitoWebViews.clear()
        
        // مسح البيانات المتبقية
        clearIncognitoData()
    }
    
    /**
     * هل يوجد تبويبات خاصة مفتوحة
     */
    fun hasOpenIncognitoTabs(): Boolean {
        return incognitoWebViews.isNotEmpty()
    }
    
    /**
     * عدد التبويبات الخاصة
     */
    fun getIncognitoTabCount(): Int {
        return incognitoWebViews.size
    }
    
    /**
     * مسح جميع بيانات التصفح (للاستخدام العام)
     */
    fun clearAllBrowsingData(
        clearCache: Boolean = true,
        clearCookies: Boolean = true,
        clearFormData: Boolean = true,
        clearWebStorage: Boolean = true
    ) {
        if (clearCookies) {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        }
        
        if (clearWebStorage) {
            webStorage.deleteAllData()
        }
        
        // مسح بيانات WebView بشكل عام
        val tempWebView = WebView(context)
        if (clearCache) {
            tempWebView.clearCache(true)
        }
        if (clearFormData) {
            tempWebView.clearFormData()
        }
        tempWebView.destroy()
    }
}
