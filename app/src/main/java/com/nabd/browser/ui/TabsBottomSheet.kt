package com.nabd.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabd.browser.models.Tab

/**
 * Bottom Sheet للتبويبات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsBottomSheet(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // العنوان وأزرار التحكم
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tabs.size} تبويب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // تبويب جديد
                    IconButton(onClick = onNewTab) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "تبويب جديد"
                        )
                    }
                    
                    // تبويب خاص جديد
                    IconButton(onClick = onNewIncognitoTab) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "تصفح خاص"
                        )
                    }
                    
                    // إغلاق الكل
                    IconButton(onClick = onCloseAllTabs) {
                        Icon(
                            imageVector = Icons.Default.CloseFullscreen,
                            contentDescription = "إغلاق الكل"
                        )
                    }
                }
            }
            
            // شبكة التبويبات
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(
                    items = tabs,
                    key = { it.id }
                ) { tab ->
                    TabCard(
                        tab = tab,
                        isSelected = tabs.indexOf(tab) == currentTabIndex,
                        onClick = { onTabSelected(tabs.indexOf(tab)) },
                        onClose = { onTabClosed(tab.id) }
                    )
                }
            }
        }
    }
}

/**
 * بطاقة التبويب
 */
@Composable
fun TabCard(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (tab.isIncognito) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // الشريط العلوي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // أيقونة الموقع
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (tab.isIncognito) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tab.isIncognito) {
                            Icons.Outlined.VisibilityOff
                        } else if (tab.isSecure) {
                            Icons.Default.Lock
                        } else {
                            Icons.Default.Language
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // زر الإغلاق
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // العنوان
            Text(
                text = tab.title.ifBlank { "تبويب جديد" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // الرابط
            Text(
                text = tab.domain,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // مؤشر التحميل
            if (tab.isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { tab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/**
 * شاشة التبويبات (بديل للـ BottomSheet)
 */
@Composable
fun TabsScreen(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${tabs.size} تبويب") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNewTab) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "تبويب جديد"
                        )
                    }
                    IconButton(onClick = onNewIncognitoTab) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "تصفح خاص"
                        )
                    }
                    IconButton(onClick = onCloseAllTabs) {
                        Icon(
                            imageVector = Icons.Default.CloseFullscreen,
                            contentDescription = "إغلاق الكل"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(
                items = tabs,
                key = { it.id }
            ) { tab ->
                TabCard(
                    tab = tab,
                    isSelected = tabs.indexOf(tab) == currentTabIndex,
                    onClick = { onTabSelected(tabs.indexOf(tab)) },
                    onClose = { onTabClosed(tab.id) }
                )
            }
        }
    }
}

/**
 * زر عدد التبويبات
 */
@Composable
fun TabCountButton(
    count: Int,
    isIncognito: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isIncognito) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isIncognito) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = "تصفح خاص",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
