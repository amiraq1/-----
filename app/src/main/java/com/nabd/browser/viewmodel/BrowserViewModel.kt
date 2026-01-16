package com.nabd.browser.viewmodel

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nabd.browser.NabdApplication
import com.nabd.browser.ai.AiRepository
import com.nabd.browser.ai.AiRequestType
import com.nabd.browser.ai.ChatMessage
import com.nabd.browser.downloads.DownloadHelper
import com.nabd.browser.incognito.IncognitoManager
import com.nabd.browser.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * حالات واجهة المتصفح
 */
enum class ScreenType {
    BROWSER,    // شاشة التصفح الرئيسية
    TABS,       // عرض التبويبات
    BOOKMARKS,  // المفضلة
    HISTORY,    // السجل
    DOWNLOADS,  // التنزيلات
    SETTINGS    // الإعدادات
}

/**
 * حالة AI Panel
 */
data class AiPanelState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val currentPageContent: String = "",
    val selectedText: String = "",
    val errorMessage: String? = null
)

/**
 * حالة المتصفح الكاملة
 */
data class BrowserState(
    val tabs: List<Tab> = listOf(Tab.createNew()),
    val currentTabIndex: Int = 0,
    val currentScreen: ScreenType = ScreenType.BROWSER,
    val isIncognitoMode: Boolean = false,
    val searchQuery: String = "",
    val isUrlBarFocused: Boolean = false,
    val showMenu: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDesktopMode: Boolean = false,
    val showTabsSheet: Boolean = false,
    val showAiPanel: Boolean = false
) {
    /**
     * التبويب الحالي
     */
    val currentTab: Tab?
        get() = tabs.getOrNull(currentTabIndex)
    
    /**
     * عدد التبويبات
     */
    val tabCount: Int
        get() = tabs.size
    
    /**
     * عدد التبويبات العادية
     */
    val normalTabCount: Int
        get() = tabs.count { !it.isIncognito }
    
    /**
     * عدد التبويبات الخاصة
     */
    val incognitoTabCount: Int
        get() = tabs.count { it.isIncognito }
}

/**
 * ViewModel للمتصفح
 * يدير حالة التطبيق بالكامل مع دعم Room وAI
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository من Application
    private val app = application as NabdApplication
    private val repository = app.repository
    
    // AI Repository
    private val aiRepository = AiRepository()
    
    // Incognito Manager
    val incognitoManager = IncognitoManager(application)
    
    // Download Helper
    val downloadHelper = DownloadHelper(application, repository)
    
    // ========================================
    // State Management
    // ========================================
    
    // حالة المتصفح الرئيسية
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()
    
    // حالة AI Panel
    private val _aiState = MutableStateFlow(AiPanelState())
    val aiState: StateFlow<AiPanelState> = _aiState.asStateFlow()
    
    // خريطة WebViews للتبويبات
    private val webViewMap = mutableMapOf<String, WebView>()
    
    // ========================================
    // بيانات من Room (Flow)
    // ========================================
    
    val bookmarks: StateFlow<List<Bookmark>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val history: StateFlow<List<HistoryItem>> = repository.getRecentHistory(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val downloads: StateFlow<List<DownloadItem>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val mostVisitedSites: StateFlow<List<HistoryItem>> = repository.getMostVisitedSites(8)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // ========================================
    // التهيئة
    // ========================================
    
    init {
        downloadHelper.register()
    }
    
    override fun onCleared() {
        super.onCleared()
        downloadHelper.unregister()
        webViewMap.values.forEach { it.destroy() }
        webViewMap.clear()
        incognitoManager.closeAllIncognitoTabs()
    }
    
    // ========================================
    // إدارة التبويبات
    // ========================================
    
    /**
     * إنشاء تبويب جديد
     */
    fun createNewTab(url: String? = null, isIncognito: Boolean = false) {
        val newTab = Tab.createNew(isIncognito).copy(
            url = url ?: Tab.DEFAULT_HOME_URL
        )
        _state.value = _state.value.copy(
            tabs = _state.value.tabs + newTab,
            currentTabIndex = _state.value.tabs.size,
            currentScreen = ScreenType.BROWSER,
            showTabsSheet = false,
            isIncognitoMode = isIncognito
        )
    }
    
    /**
     * تحديد التبويب الحالي
     */
    fun selectTab(index: Int) {
        if (index in 0 until _state.value.tabs.size) {
            val tab = _state.value.tabs[index]
            _state.value = _state.value.copy(
                currentTabIndex = index,
                currentScreen = ScreenType.BROWSER,
                showTabsSheet = false,
                isIncognitoMode = tab.isIncognito
            )
        }
    }
    
    /**
     * تحديد التبويب بواسطة المعرف
     */
    fun selectTabById(tabId: String) {
        val index = _state.value.tabs.indexOfFirst { it.id == tabId }
        if (index >= 0) selectTab(index)
    }
    
    /**
     * إغلاق تبويب
     */
    fun closeTab(tabId: String) {
        val tabs = _state.value.tabs
        val index = tabs.indexOfFirst { it.id == tabId }
        val tab = tabs.getOrNull(index) ?: return
        
        if (tabs.size > 1) {
            // إزالة WebView
            webViewMap.remove(tabId)?.let { webView ->
                if (tab.isIncognito) {
                    incognitoManager.unregisterIncognitoWebView(webView)
                }
                webView.destroy()
            }
            
            val newTabs = tabs.filterNot { it.id == tabId }
            val newIndex = when {
                index >= newTabs.size -> newTabs.size - 1
                else -> index
            }
            
            _state.value = _state.value.copy(
                tabs = newTabs,
                currentTabIndex = newIndex,
                isIncognitoMode = newTabs[newIndex].isIncognito
            )
        } else {
            // إذا كان التبويب الوحيد، أنشئ تبويبًا جديدًا
            webViewMap.remove(tabId)?.let { webView ->
                if (tab.isIncognito) {
                    incognitoManager.unregisterIncognitoWebView(webView)
                }
                webView.destroy()
            }
            _state.value = _state.value.copy(
                tabs = listOf(Tab.createNew()),
                currentTabIndex = 0,
                isIncognitoMode = false
            )
        }
    }
    
    /**
     * إغلاق جميع التبويبات
     */
    fun closeAllTabs(incognitoOnly: Boolean = false) {
        if (incognitoOnly) {
            // إغلاق التبويبات الخاصة فقط
            val incognitoTabs = _state.value.tabs.filter { it.isIncognito }
            incognitoTabs.forEach { tab ->
                webViewMap.remove(tab.id)?.let { webView ->
                    incognitoManager.unregisterIncognitoWebView(webView)
                    webView.destroy()
                }
            }
            
            val remainingTabs = _state.value.tabs.filterNot { it.isIncognito }
            val newTabs = if (remainingTabs.isEmpty()) listOf(Tab.createNew()) else remainingTabs
            
            _state.value = _state.value.copy(
                tabs = newTabs,
                currentTabIndex = 0,
                isIncognitoMode = false
            )
            
            incognitoManager.clearIncognitoData()
        } else {
            webViewMap.values.forEach { it.destroy() }
            webViewMap.clear()
            _state.value = _state.value.copy(
                tabs = listOf(Tab.createNew()),
                currentTabIndex = 0,
                isIncognitoMode = false
            )
        }
    }
    
    /**
     * إظهار/إخفاء قائمة التبويبات
     */
    fun toggleTabsSheet() {
        _state.value = _state.value.copy(showTabsSheet = !_state.value.showTabsSheet)
    }
    
    fun hideTabsSheet() {
        _state.value = _state.value.copy(showTabsSheet = false)
    }
    
    // ========================================
    // التنقل (Navigation)
    // ========================================
    
    /**
     * تحميل رابط في التبويب الحالي
     */
    fun loadUrl(url: String) {
        val processedUrl = processUrl(url)
        updateCurrentTab { it.copy(url = processedUrl, isLoading = true) }
        getWebViewForCurrentTab()?.loadUrl(processedUrl)
    }
    
    /**
     * الرجوع للخلف
     */
    fun goBack() {
        getWebViewForCurrentTab()?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }
    
    /**
     * التقدم للأمام
     */
    fun goForward() {
        getWebViewForCurrentTab()?.let { webView ->
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }
    }
    
    /**
     * تحديث الصفحة
     */
    fun reload() {
        getWebViewForCurrentTab()?.reload()
    }
    
    /**
     * إيقاف التحميل
     */
    fun stopLoading() {
        getWebViewForCurrentTab()?.stopLoading()
        updateCurrentTab { it.copy(isLoading = false) }
    }
    
    /**
     * الذهاب للصفحة الرئيسية
     */
    fun goHome() {
        loadUrl(Tab.DEFAULT_HOME_URL)
    }
    
    // ========================================
    // تحديث حالة التبويب
    // ========================================
    
    /**
     * تحديث التبويب الحالي
     */
    fun updateCurrentTab(update: (Tab) -> Tab) {
        _state.value.currentTab?.let { currentTab ->
            val updatedTab = update(currentTab)
            val newTabs = _state.value.tabs.toMutableList()
            newTabs[_state.value.currentTabIndex] = updatedTab
            _state.value = _state.value.copy(tabs = newTabs)
        }
    }
    
    /**
     * تحديث تقدم التحميل
     */
    fun updateProgress(progress: Int) {
        updateCurrentTab { it.copy(progress = progress, isLoading = progress < 100) }
    }
    
    /**
     * تحديث العنوان
     */
    fun updateTitle(title: String) {
        updateCurrentTab { it.copy(title = title) }
    }
    
    /**
     * تحديث الرابط وإضافة للسجل
     */
    fun updateUrl(url: String) {
        val currentTab = _state.value.currentTab ?: return
        updateCurrentTab { it.copy(url = url) }
        
        // إضافة للسجل (فقط إذا لم يكن وضع خاص)
        if (!currentTab.isIncognito && url != Tab.BLANK_PAGE) {
            viewModelScope.launch {
                repository.addToHistory(currentTab.copy(url = url))
            }
        }
    }
    
    /**
     * تحديث إمكانية التنقل
     */
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        updateCurrentTab { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }
    
    // ========================================
    // إدارة WebView
    // ========================================
    
    /**
     * تسجيل WebView لتبويب
     */
    fun registerWebView(tabId: String, webView: WebView) {
        webViewMap[tabId] = webView
        
        // تسجيل مع IncognitoManager إذا كان تبويب خاص
        val tab = _state.value.tabs.find { it.id == tabId }
        if (tab?.isIncognito == true) {
            incognitoManager.registerIncognitoWebView(webView)
        }
    }
    
    /**
     * الحصول على WebView للتبويب الحالي
     */
    fun getWebViewForCurrentTab(): WebView? {
        return _state.value.currentTab?.let { webViewMap[it.id] }
    }
    
    /**
     * الحصول على WebView لتبويب معين
     */
    fun getWebViewForTab(tabId: String): WebView? {
        return webViewMap[tabId]
    }
    
    // ========================================
    // المفضلة (مع Room)
    // ========================================
    
    /**
     * التحقق من وجود الصفحة الحالية في المفضلة
     */
    suspend fun isCurrentPageBookmarked(): Boolean {
        return _state.value.currentTab?.let { tab ->
            repository.isBookmarked(tab.url)
        } ?: false
    }
    
    /**
     * تبديل حالة المفضلة للصفحة الحالية
     */
    fun toggleBookmark() {
        _state.value.currentTab?.let { tab ->
            viewModelScope.launch {
                if (repository.isBookmarked(tab.url)) {
                    repository.removeBookmark(tab.url)
                } else {
                    repository.addBookmark(tab)
                }
            }
        }
    }
    
    /**
     * إضافة للمفضلة
     */
    fun addBookmark() {
        _state.value.currentTab?.let { tab ->
            viewModelScope.launch {
                if (!repository.isBookmarked(tab.url)) {
                    repository.addBookmark(tab)
                }
            }
        }
    }
    
    /**
     * حذف مفضلة
     */
    fun removeBookmark(url: String) {
        viewModelScope.launch {
            repository.removeBookmark(url)
        }
    }
    
    /**
     * فتح مفضلة
     */
    fun openBookmark(bookmark: Bookmark, inNewTab: Boolean = false) {
        if (inNewTab) {
            createNewTab(bookmark.url)
        } else {
            loadUrl(bookmark.url)
        }
        _state.value = _state.value.copy(currentScreen = ScreenType.BROWSER)
    }
    
    /**
     * مسح جميع المفضلات
     */
    fun clearAllBookmarks() {
        viewModelScope.launch {
            repository.clearAllBookmarks()
        }
    }
    
    // ========================================
    // السجل (مع Room)
    // ========================================
    
    /**
     * فتح عنصر من السجل
     */
    fun openHistoryItem(item: HistoryItem, inNewTab: Boolean = false) {
        if (inNewTab) {
            createNewTab(item.url)
        } else {
            loadUrl(item.url)
        }
        _state.value = _state.value.copy(currentScreen = ScreenType.BROWSER)
    }
    
    /**
     * حذف عنصر من السجل
     */
    fun deleteHistoryItem(itemId: String) {
        viewModelScope.launch {
            repository.removeHistoryItem(itemId)
        }
    }
    
    /**
     * مسح السجل بالكامل
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
    
    // ========================================
    // التنزيلات
    // ========================================
    
    /**
     * إضافة تنزيل جديد
     */
    fun addDownload(url: String, fileName: String, mimeType: String? = null) {
        downloadHelper.startDownload(url, null, mimeType)
    }
    
    /**
     * فتح ملف منزل
     */
    fun openDownload(download: DownloadItem) {
        download.filePath?.let { path ->
            downloadHelper.openDownloadedFile(path, download.mimeType)
        }
    }
    
    /**
     * حذف تنزيل من السجل
     */
    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            repository.removeDownload(downloadId)
        }
    }
    
    /**
     * مسح جميع التنزيلات
     */
    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }
    
    // ========================================
    // AI Panel
    // ========================================
    
    /**
     * إظهار/إخفاء AI Panel
     */
    fun toggleAiPanel() {
        _state.value = _state.value.copy(showAiPanel = !_state.value.showAiPanel)
        
        if (_state.value.showAiPanel) {
            // استخراج محتوى الصفحة
            extractPageContent()
        }
    }
    
    fun hideAiPanel() {
        _state.value = _state.value.copy(showAiPanel = false)
    }
    
    /**
     * استخراج محتوى الصفحة
     */
    fun extractPageContent() {
        getWebViewForCurrentTab()?.evaluateJavascript(
            "(function() { return document.body.innerText; })();"
        ) { result ->
            val content = result?.removeSurrounding("\"")?.replace("\\n", "\n") ?: ""
            _aiState.value = _aiState.value.copy(currentPageContent = content)
        }
    }
    
    /**
     * استخراج النص المحدد
     */
    fun extractSelectedText() {
        getWebViewForCurrentTab()?.evaluateJavascript(
            "(function() { return window.getSelection().toString(); })();"
        ) { result ->
            val text = result?.removeSurrounding("\"") ?: ""
            _aiState.value = _aiState.value.copy(selectedText = text)
        }
    }
    
    /**
     * تلخيص الصفحة
     */
    fun summarizePage(apiKey: String? = null) {
        val content = _aiState.value.currentPageContent
        if (content.isBlank()) {
            _aiState.value = _aiState.value.copy(errorMessage = "لا يوجد محتوى للتلخيص")
            return
        }
        
        _aiState.value = _aiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = aiRepository.summarizePage(
                pageContent = content.take(15000),
                apiKey = apiKey ?: com.nabd.browser.ai.AiServiceProvider.AI_API_KEY
            )
            
            result.fold(
                onSuccess = { response ->
                    addAiMessage(content = "لخص هذه الصفحة", isUser = true)
                    addAiMessage(content = response, isUser = false)
                    _aiState.value = _aiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _aiState.value = _aiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "حدث خطأ"
                    )
                }
            )
        }
    }
    
    /**
     * شرح النص المحدد
     */
    fun explainSelection(apiKey: String? = null) {
        extractSelectedText()
        
        viewModelScope.launch {
            // انتظر قليلاً للحصول على النص المحدد
            kotlinx.coroutines.delay(300)
            
            val text = _aiState.value.selectedText
            if (text.isBlank()) {
                _aiState.value = _aiState.value.copy(errorMessage = "الرجاء تحديد نص أولاً")
                return@launch
            }
            
            _aiState.value = _aiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = aiRepository.explainSelection(
                selectedText = text,
                pageContext = _aiState.value.currentPageContent.take(5000),
                apiKey = apiKey ?: com.nabd.browser.ai.AiServiceProvider.AI_API_KEY
            )
            
            result.fold(
                onSuccess = { response ->
                    addAiMessage(content = "اشرح: $text", isUser = true)
                    addAiMessage(content = response, isUser = false)
                    _aiState.value = _aiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _aiState.value = _aiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "حدث خطأ"
                    )
                }
            )
        }
    }
    
    /**
     * طرح سؤال عن الصفحة
     */
    fun askAboutPage(question: String, apiKey: String? = null) {
        if (question.isBlank()) return
        
        val content = _aiState.value.currentPageContent
        if (content.isBlank()) {
            _aiState.value = _aiState.value.copy(errorMessage = "لا يوجد محتوى للاستفسار عنه")
            return
        }
        
        addAiMessage(content = question, isUser = true)
        _aiState.value = _aiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = aiRepository.askAboutPage(
                question = question,
                pageContent = content.take(15000),
                previousMessages = _aiState.value.messages,
                apiKey = apiKey ?: com.nabd.browser.ai.AiServiceProvider.AI_API_KEY
            )
            
            result.fold(
                onSuccess = { response ->
                    addAiMessage(content = response, isUser = false)
                    _aiState.value = _aiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _aiState.value = _aiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "حدث خطأ"
                    )
                }
            )
        }
    }
    
    /**
     * إضافة رسالة للدردشة
     */
    private fun addAiMessage(content: String, isUser: Boolean) {
        val message = ChatMessage(content = content, isUser = isUser)
        _aiState.value = _aiState.value.copy(
            messages = _aiState.value.messages + message
        )
    }
    
    /**
     * مسح رسائل AI
     */
    fun clearAiMessages() {
        _aiState.value = _aiState.value.copy(messages = emptyList(), errorMessage = null)
    }
    
    // ========================================
    // التنقل بين الشاشات
    // ========================================
    
    /**
     * تغيير الشاشة الحالية
     */
    fun navigateTo(screen: ScreenType) {
        _state.value = _state.value.copy(currentScreen = screen)
    }
    
    /**
     * إظهار/إخفاء القائمة
     */
    fun toggleMenu() {
        _state.value = _state.value.copy(showMenu = !_state.value.showMenu)
    }
    
    /**
     * إخفاء القائمة
     */
    fun hideMenu() {
        _state.value = _state.value.copy(showMenu = false)
    }
    
    /**
     * تحديث استعلام البحث
     */
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }
    
    /**
     * تحديث حالة تركيز شريط العناوين
     */
    fun updateUrlBarFocus(focused: Boolean) {
        _state.value = _state.value.copy(isUrlBarFocused = focused)
    }
    
    // ========================================
    // الإعدادات
    // ========================================
    
    /**
     * تبديل الوضع الداكن
     */
    fun toggleDarkMode() {
        _state.value = _state.value.copy(isDarkMode = !_state.value.isDarkMode)
    }
    
    /**
     * تبديل وضع سطح المكتب
     */
    fun toggleDesktopMode() {
        _state.value = _state.value.copy(isDesktopMode = !_state.value.isDesktopMode)
        getWebViewForCurrentTab()?.let { webView ->
            webView.settings.apply {
                userAgentString = if (_state.value.isDesktopMode) {
                    DESKTOP_USER_AGENT
                } else {
                    null
                }
            }
            webView.reload()
        }
    }
    
    /**
     * مسح جميع بيانات التصفح
     */
    fun clearBrowsingData(
        clearHistory: Boolean = true,
        clearBookmarks: Boolean = false,
        clearDownloads: Boolean = false,
        clearCache: Boolean = true,
        clearCookies: Boolean = true
    ) {
        viewModelScope.launch {
            if (clearHistory) repository.clearAllHistory()
            if (clearBookmarks) repository.clearAllBookmarks()
            if (clearDownloads) repository.clearAllDownloads()
        }
        
        incognitoManager.clearAllBrowsingData(
            clearCache = clearCache,
            clearCookies = clearCookies,
            clearFormData = true,
            clearWebStorage = true
        )
    }
    
    // ========================================
    // مساعدات
    // ========================================
    
    /**
     * معالجة الرابط المُدخل
     */
    private fun processUrl(input: String): String {
        val trimmed = input.trim()
        
        return when {
            // رابط كامل
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            
            // رابط بدون بروتوكول
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            
            // بحث Google
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
        }
    }
    
    companion object {
        private const val DESKTOP_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
