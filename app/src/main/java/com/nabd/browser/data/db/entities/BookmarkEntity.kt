package com.nabd.browser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان المفضلة في قاعدة البيانات
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
