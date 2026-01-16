package com.nabd.browser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان السجل في قاعدة البيانات
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val visitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)
