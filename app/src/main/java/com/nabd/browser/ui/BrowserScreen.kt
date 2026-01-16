package com.nabd.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nabd.browser.models.Tab
import com.nabd.browser.viewmodel.BrowserState
import com.nabd.browser.viewmodel.BrowserViewModel
import com.nabd.browser.viewmodel.ScreenType

/**
 * شاشة المتصفح الرئيسية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    
    // حالة المفضلة للصفحة الحالية
    var isCurrentPageBookmarked by remember { mutableStateOf(false) }
    LaunchedEffect(state.currentTab?.url) {
        isCurrentPageBookmarked = viewModel.isCurrentPageBookmarked()
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when (state.currentScreen) {
            ScreenType.BROWSER -> {
                BrowserContent(
                    state = state,
                    isBookmarked = isCurrentPageBookmarked,
                    viewModel = viewModel
                )
            }
            
            ScreenType.TABS -> {
                TabsScreen(
                    tabs = state.tabs,
                    currentTabIndex = state.currentTabIndex,
                    onTabSelected = { viewModel.selectTab(it) },
                    onTabClosed = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.createNewTab() },
                    onNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
                    onCloseAllTabs = { viewModel.closeAllTabs() },
                    onBack = { viewModel.navigateTo(ScreenType.BROWSER) }
                )
            }
            
            ScreenType.BOOKMARKS -> {
                BookmarksScreen(
                    bookmarks = bookmarks,
                    onBookmarkClick = { viewModel.openBookmark(it) },
                    onBookmarkLongClick = { viewModel.openBookmark(it, inNewTab = true) },
                    onDeleteBookmark = { viewModel.removeBookmark(bookmarks.find { b -> b.id == it }?.url ?: "") },
                    onClearAll = { viewModel.clearAllBookmarks() },
                    onBack = { viewModel.navigateTo(ScreenType.BROWSER) }
                )
            }
            
            ScreenType.HISTORY -> {
                HistoryScreen(
                    history = history,
                    onHistoryClick = { viewModel.openHistoryItem(it) },
                    onDeleteItem = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearHistory() },
                    onBack = { viewModel.navigateTo(ScreenType.BROWSER) }
                )
            }
            
            ScreenType.DOWNLOADS -> {
                DownloadsScreen(
                    downloads = downloads,
                    onDownloadClick = { viewModel.openDownload(it) },
                    onDeleteDownload = { viewModel.deleteDownload(it) },
                    onClearAll = { viewModel.clearDownloads() },
                    onBack = { viewModel.navigateTo(ScreenType.BROWSER) }
                )
            }
            
            ScreenType.SETTINGS -> {
                SettingsScreen(
                    isDarkMode = state.isDarkMode,
                    isDesktopMode = state.isDesktopMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                    onClearBrowsingData = { viewModel.clearBrowsingData() },
                    onBack = { viewModel.navigateTo(ScreenType.BROWSER) }
                )
            }
        }
        
        // TabsBottomSheet
        if (state.showTabsSheet) {
            TabsBottomSheet(
                tabs = state.tabs,
                currentTabIndex = state.currentTabIndex,
                onTabSelected = { viewModel.selectTab(it) },
                onTabClosed = { viewModel.closeTab(it) },
                onNewTab = { viewModel.createNewTab() },
                onNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
                onCloseAllTabs = { viewModel.closeAllTabs() },
                onDismiss = { viewModel.hideTabsSheet() }
            )
        }
        
        // AI Panel BottomSheet
        if (state.showAiPanel) {
            AiPanelBottomSheet(
                aiState = aiState,
                onSummarize = { viewModel.summarizePage() },
                onExplainSelection = { viewModel.explainSelection() },
                onAskQuestion = { viewModel.askAboutPage(it) },
                onClearMessages = { viewModel.clearAiMessages() },
                onDismiss = { viewModel.hideAiPanel() }
            )
        }
    }
}

/**
 * محتوى المتصفح الرئيسي
 */
@Composable
fun BrowserContent(
    state: BrowserState,
    isBookmarked: Boolean,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab = state.currentTab ?: return
    var urlInput by remember(currentTab.url) { mutableStateOf(currentTab.url) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isUrlFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // شريط العنوان
        UrlBar(
            url = urlInput,
            onUrlChange = { urlInput = it },
            onGo = {
                viewModel.loadUrl(urlInput)
                focusManager.clearFocus()
            },
            isLoading = currentTab.isLoading,
            isSecure = currentTab.isSecure,
            isIncognito = currentTab.isIncognito,
            onStopLoading = { viewModel.stopLoading() },
            focusRequester = focusRequester,
            onFocusChanged = { isUrlFocused = it }
        )
        
        // شريط التقدم
        LoadingProgressBar(
            progress = currentTab.progress,
            isLoading = currentTab.isLoading
        )
        
        // WebView
        Box(modifier = Modifier.weight(1f)) {
            WebViewContainer(
                tabId = currentTab.id,
                initialUrl = currentTab.url,
                viewModel = viewModel,
                isIncognito = currentTab.isIncognito,
                isDesktopMode = state.isDesktopMode
            )
            
            // طبقة Incognito
            if (currentTab.isIncognito) {
                IncognitoBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
        
        // شريط التنقل
        NavigationBar(
            canGoBack = currentTab.canGoBack,
            canGoForward = currentTab.canGoForward,
            tabCount = state.tabCount,
            isIncognito = state.isIncognitoMode,
            isBookmarked = isBookmarked,
            onBack = { viewModel.goBack() },
            onForward = { viewModel.goForward() },
            onReload = { viewModel.reload() },
            onHome = { viewModel.goHome() },
            onTabs = { viewModel.toggleTabsSheet() },
            onBookmark = { viewModel.toggleBookmark() },
            onMenu = { viewModel.toggleMenu() }
        )
        
        // القائمة
        DropdownMenu(
            expanded = state.showMenu,
            onDismissRequest = { viewModel.hideMenu() }
        ) {
            BrowserMenu(
                isIncognito = state.isIncognitoMode,
                isDesktopMode = state.isDesktopMode,
                onNewTab = {
                    viewModel.createNewTab()
                    viewModel.hideMenu()
                },
                onNewIncognitoTab = {
                    viewModel.createNewTab(isIncognito = true)
                    viewModel.hideMenu()
                },
                onBookmarks = {
                    viewModel.navigateTo(ScreenType.BOOKMARKS)
                    viewModel.hideMenu()
                },
                onHistory = {
                    viewModel.navigateTo(ScreenType.HISTORY)
                    viewModel.hideMenu()
                },
                onDownloads = {
                    viewModel.navigateTo(ScreenType.DOWNLOADS)
                    viewModel.hideMenu()
                },
                onToggleDesktopMode = {
                    viewModel.toggleDesktopMode()
                    viewModel.hideMenu()
                },
                onAiAssistant = {
                    viewModel.extractPageContent()
                    viewModel.toggleAiPanel()
                    viewModel.hideMenu()
                },
                onSettings = {
                    viewModel.navigateTo(ScreenType.SETTINGS)
                    viewModel.hideMenu()
                }
            )
        }
    }
}

/**
 * شريط العنوان
 */
@Composable
fun UrlBar(
    url: String,
    onUrlChange: (String) -> Unit,
    onGo: () -> Unit,
    isLoading: Boolean,
    isSecure: Boolean,
    isIncognito: Boolean,
    onStopLoading: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isIncognito) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // أيقونة الأمان/التصفح الخاص
            Icon(
                imageVector = when {
                    isIncognito -> Icons.Outlined.VisibilityOff
                    isSecure -> Icons.Default.Lock
                    else -> Icons.Default.Language
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when {
                    isIncognito -> MaterialTheme.colorScheme.secondary
                    isSecure -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // حقل الإدخال
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                singleLine = true,
                placeholder = { Text("ابحث أو أدخل رابط...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            // زر Go أو Stop
            FilledIconButton(
                onClick = if (isLoading) onStopLoading else onGo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isLoading) "إيقاف" else "بحث"
                )
            }
        }
    }
}

/**
 * شريط التنقل
 */
@Composable
fun NavigationBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    isIncognito: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onBookmark: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // رجوع
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = if (canGoBack) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            // تقدم
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "تقدم",
                    tint = if (canGoForward) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            // تحديث
            IconButton(onClick = onReload) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث"
                )
            }
            
            // الرئيسية
            IconButton(onClick = onHome) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "الرئيسية"
                )
            }
            
            // المفضلة
            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "المفضلة",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // التبويبات
            TabCountButton(
                count = tabCount,
                isIncognito = isIncognito,
                onClick = onTabs
            )
            
            // القائمة
            IconButton(onClick = onMenu) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "المزيد"
                )
            }
        }
    }
}

/**
 * قائمة المتصفح
 */
@Composable
fun BrowserMenu(
    isIncognito: Boolean,
    isDesktopMode: Boolean,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onAiAssistant: () -> Unit,
    onSettings: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("تبويب جديد") },
        onClick = onNewTab,
        leadingIcon = { Icon(Icons.Default.Add, null) }
    )
    DropdownMenuItem(
        text = { Text("تصفح خاص") },
        onClick = onNewIncognitoTab,
        leadingIcon = { Icon(Icons.Outlined.VisibilityOff, null) }
    )
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text("المفضلة") },
        onClick = onBookmarks,
        leadingIcon = { Icon(Icons.Outlined.Bookmarks, null) }
    )
    DropdownMenuItem(
        text = { Text("السجل") },
        onClick = onHistory,
        leadingIcon = { Icon(Icons.Outlined.History, null) }
    )
    DropdownMenuItem(
        text = { Text("التنزيلات") },
        onClick = onDownloads,
        leadingIcon = { Icon(Icons.Outlined.Download, null) }
    )
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text("مساعد AI") },
        onClick = onAiAssistant,
        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
        trailingIcon = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "جديد",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    )
    DropdownMenuItem(
        text = { Text(if (isDesktopMode) "وضع الهاتف" else "وضع سطح المكتب") },
        onClick = onToggleDesktopMode,
        leadingIcon = {
            Icon(
                if (isDesktopMode) Icons.Outlined.PhoneAndroid else Icons.Outlined.Computer,
                null
            )
        }
    )
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text("الإعدادات") },
        onClick = onSettings,
        leadingIcon = { Icon(Icons.Outlined.Settings, null) }
    )
}

/**
 * شارة التصفح الخاص
 */
@Composable
fun IncognitoBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
            Text(
                text = "خاص",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

/**
 * شاشة الإعدادات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    isDesktopMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onClearBrowsingData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // الوضع الداكن
            ListItem(
                headlineContent = { Text("الوضع الداكن") },
                leadingContent = {
                    Icon(Icons.Outlined.DarkMode, null)
                },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            )
            
            // وضع سطح المكتب
            ListItem(
                headlineContent = { Text("وضع سطح المكتب") },
                supportingContent = { Text("عرض المواقع كما في الحاسوب") },
                leadingContent = {
                    Icon(Icons.Outlined.Computer, null)
                },
                trailingContent = {
                    Switch(
                        checked = isDesktopMode,
                        onCheckedChange = { onToggleDesktopMode() }
                    )
                }
            )
            
            HorizontalDivider()
            
            // مسح بيانات التصفح
            ListItem(
                headlineContent = { Text("مسح بيانات التصفح") },
                supportingContent = { Text("حذف السجل والكوكيز والتخزين المؤقت") },
                leadingContent = {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onClearBrowsingData)
            )
        }
    }
}
