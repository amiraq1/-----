package com.nabd.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.nabd.browser.ui.BrowserScreen
import com.nabd.browser.ui.theme.NabdBrowserTheme
import com.nabd.browser.viewmodel.BrowserViewModel

/**
 * النشاط الرئيسي لمتصفح نبض
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: BrowserViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // تفعيل Edge-to-Edge
        enableEdgeToEdge()
        
        // إنشاء ViewModel
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        
        // التعامل مع Intent (فتح روابط خارجية)
        handleIntent()
        
        setContent {
            NabdBrowserApp(viewModel = viewModel)
        }
    }
    
    /**
     * معالجة Intent لفتح الروابط
     */
    private fun handleIntent() {
        intent?.data?.toString()?.let { url ->
            if (url.startsWith("http://") || url.startsWith("https://")) {
                viewModel.loadUrl(url)
            }
        }
    }
    
    /**
     * معالجة زر الرجوع
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView = viewModel.getWebViewForCurrentTab()
        when {
            webView?.canGoBack() == true -> webView.goBack()
            viewModel.state.value.tabs.size > 1 -> {
                viewModel.closeTab(viewModel.state.value.currentTab?.id ?: "")
            }
            else -> super.onBackPressed()
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.getWebViewForCurrentTab()?.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.getWebViewForCurrentTab()?.onPause()
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event?.isCtrlPressed == true) {
            when (keyCode) {
                // Ctrl + T: تبويب جديد
                android.view.KeyEvent.KEYCODE_T -> {
                    viewModel.createNewTab()
                    return true
                }
                // Ctrl + W: إغلاق التبويب
                android.view.KeyEvent.KEYCODE_W -> {
                    viewModel.state.value.currentTab?.let { viewModel.closeTab(it.id) }
                    return true
                }
                // Ctrl + R: تحديث
                android.view.KeyEvent.KEYCODE_R -> {
                    viewModel.reload()
                    return true
                }
                // Ctrl + L: تركيز شريط العنوان (يتطلب منطق إضافي في UI)
                // Ctrl + Shift + N: تصفح خاص
                android.view.KeyEvent.KEYCODE_N -> {
                    if (event.isShiftPressed) {
                        viewModel.createNewTab(isIncognito = true)
                        return true
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

/**
 * التطبيق الرئيسي
 */
@Composable
fun NabdBrowserApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsState()
    
    NabdBrowserTheme(darkTheme = state.isDarkMode) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            BrowserScreen(viewModel = viewModel)
        }
    }
}
