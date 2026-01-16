package com.nabd.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nabd.browser.models.Bookmark
import com.nabd.browser.models.DownloadItem
import com.nabd.browser.models.DownloadStatus
import com.nabd.browser.models.HistoryItem

/**
 * شاشة المفضلة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkLongClick: (Bookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المفضلة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, "مسح الكل")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (bookmarks.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "لا توجد مفضلات",
                message = "أضف صفحات للمفضلة للوصول إليها بسرعة",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark) },
                        onDelete = { onDeleteBookmark(bookmark.id) }
                    )
                }
            }
        }
    }
}

/**
 * عنصر المفضلة
 */
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = bookmark.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = bookmark.domain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

/**
 * شاشة السجل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    onHistoryClick: (HistoryItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("السجل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, "مسح السجل")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "لا يوجد سجل",
                message = "سيظهر سجل التصفح هنا",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // تجميع حسب التاريخ
            val groupedHistory = remember(history) {
                HistoryItem.groupByDate(history)
            }
            
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                groupedHistory.forEach { (date, items) ->
                    item {
                        HistoryDateHeader(date = date)
                    }
                    items(items, key = { it.id }) { item ->
                        HistoryItemRow(
                            item = item,
                            onClick = { onHistoryClick(item) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * عنوان تاريخ السجل
 */
@Composable
fun HistoryDateHeader(
    date: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * عنصر السجل
 */
@Composable
fun HistoryItemRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.title.ifBlank { item.url },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.formattedTime,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = item.domain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "حذف"
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * شاشة التنزيلات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    onDownloadClick: (DownloadItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التنزيلات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, "مسح الكل")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Download,
                title = "لا توجد تنزيلات",
                message = "ستظهر الملفات المُنزّلة هنا",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloads, key = { it.id }) { download ->
                    DownloadItemRow(
                        download = download,
                        onClick = { onDownloadClick(download) },
                        onDelete = { onDeleteDownload(download.id) }
                    )
                }
            }
        }
    }
}

/**
 * عنصر التنزيل
 */
@Composable
fun DownloadItemRow(
    download: DownloadItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (download.fileType) {
        com.nabd.browser.models.FileType.IMAGE -> Icons.Outlined.Image
        com.nabd.browser.models.FileType.VIDEO -> Icons.Outlined.VideoFile
        com.nabd.browser.models.FileType.AUDIO -> Icons.Outlined.AudioFile
        com.nabd.browser.models.FileType.PDF -> Icons.Outlined.PictureAsPdf
        com.nabd.browser.models.FileType.ARCHIVE -> Icons.Outlined.FolderZip
        else -> Icons.Outlined.InsertDriveFile
    }
    
    val statusText = when (download.status) {
        DownloadStatus.PENDING -> "قيد الانتظار"
        DownloadStatus.DOWNLOADING -> "جاري التنزيل... ${download.progress}%"
        DownloadStatus.PAUSED -> "متوقف"
        DownloadStatus.COMPLETED -> download.formattedSize
        DownloadStatus.FAILED -> "فشل: ${download.errorMessage ?: "خطأ غير معروف"}"
        DownloadStatus.CANCELLED -> "ملغي"
    }
    
    ListItem(
        headlineContent = {
            Text(
                text = download.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (download.status) {
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (download.status == DownloadStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier.clickable(
            enabled = download.status == DownloadStatus.COMPLETED,
            onClick = onClick
        )
    )
    HorizontalDivider()
}

/**
 * حالة فارغة عامة
 */
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
