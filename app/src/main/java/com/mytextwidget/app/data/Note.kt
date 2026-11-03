package com.mytextwidget.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun displayTitle(): String {
        if (title.isNotBlank()) {
            return title.trim()
        }
        val firstLine = content.lines().firstOrNull { it.isNotBlank() }?.trim()
        return if (!firstLine.isNullOrBlank()) {
            firstLine
        } else {
            "Untitled note"
        }
    }
}
