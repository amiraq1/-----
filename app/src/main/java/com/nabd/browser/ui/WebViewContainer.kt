package com.nabd.browser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nabd.browser.viewmodel.BrowserViewModel

/**
 * حاوية WebView منفصلة ونظيفة
 * تتعامل فقط مع عرض وإدارة WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tabId: String,
    initialUrl: String,
    viewModel: BrowserViewModel,
    isIncognito: Boolean = false,
    isDesktopMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // إنشاء WebView مرة واحدة لكل تبويب
    val webView = remember(tabId) {
        createWebView(
            context = context,
            tabId = tabId,
            viewModel = viewModel,
            isIncognito = isIncognito,
            isDesktopMode = isDesktopMode
        )
    }
    
    // تحديث إعدادات Desktop Mode
    LaunchedEffect(isDesktopMode) {
        webView.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else null
    }
    
    // تحميل الرابط الأولي
    LaunchedEffect(tabId, initialUrl) {
        if (webView.url != initialUrl && initialUrl.isNotBlank()) {
            webView.loadUrl(initialUrl)
        }
    }
    
    // تسجيل WebView
    DisposableEffect(tabId) {
        viewModel.registerWebView(tabId, webView)
        onDispose { }
    }
    
    // عرض WebView
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * إنشاء WebView مع جميع الإعدادات
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    tabId: String,
    viewModel: BrowserViewModel,
    isIncognito: Boolean,
    isDesktopMode: Boolean
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // إعدادات WebView
        settings.apply {
            // JavaScript
            javaScriptEnabled = true
            
            // DOM Storage
            domStorageEnabled = true
            
            // الوسائط المتعددة
            mediaPlaybackRequiresUserGesture = false
            
            // التخزين المؤقت
            cacheMode = if (isIncognito) {
                WebSettings.LOAD_NO_CACHE
            } else {
                WebSettings.LOAD_DEFAULT
            }
            
            // الوصول للملفات
            allowFileAccess = true
            allowContentAccess = true
            
            // تحجيم الصفحات
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            
            // قواعد البيانات
            databaseEnabled = !isIncognito
            
            // حفظ النماذج
            saveFormData = !isIncognito
            
            // User Agent
            if (isDesktopMode) {
                userAgentString = DESKTOP_USER_AGENT
            }
            
            // النوافذ المتعددة
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            
            // Mixed Content
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        
        // WebViewClient
        webViewClient = NabdWebViewClient(viewModel, isIncognito)
        
        // WebChromeClient
        webChromeClient = NabdWebChromeClient(viewModel)
        
        // التمرير
        isScrollbarFadingEnabled = true
        scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        
        // التنزيلات
        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            viewModel.downloadHelper.startDownload(url, contentDisposition, mimeType)
        }
        
        // مسح البيانات للوضع الخاص
        if (isIncognito) {
            clearCache(true)
            clearFormData()
            clearHistory()
        }
    }
}

/**
 * WebViewClient مخصص
 */
private class NabdWebViewClient(
    private val viewModel: BrowserViewModel,
    private val isIncognito: Boolean
) : WebViewClient() {
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { viewModel.updateUrl(it) }
        viewModel.updateProgress(0)
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        viewModel.updateProgress(100)
        view?.let {
            viewModel.updateNavigationState(it.canGoBack(), it.canGoForward())
            viewModel.updateTitle(it.title ?: "")
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.let { uri ->
            val url = uri.toString()
            
            when {
                url.startsWith("tel:") || 
                url.startsWith("mailto:") || 
                url.startsWith("sms:") -> {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        view?.context?.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
                
                url.startsWith("intent:") -> {
                    return true
                }
            }
        }
        
        return false
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
    }
    
    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        handler?.cancel()
    }
}

/**
 * WebChromeClient مخصص
 */
private class NabdWebChromeClient(
    private val viewModel: BrowserViewModel
) : WebChromeClient() {
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        viewModel.updateProgress(newProgress)
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { viewModel.updateTitle(it) }
    }
    
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
    }
    
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        if (isUserGesture) {
            viewModel.createNewTab()
        }
        return false
    }
    
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return false
    }
    
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, false, false)
    }
    
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return super.onConsoleMessage(consoleMessage)
    }
}

/**
 * شريط تقدم التحميل
 */
@Composable
fun LoadingProgressBar(
    progress: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading && progress < 100) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}

private const val DESKTOP_USER_AGENT = 
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
