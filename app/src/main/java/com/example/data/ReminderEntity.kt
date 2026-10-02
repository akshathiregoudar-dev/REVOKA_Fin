package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ContentMode {
    RECORD,
    TYPE
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobCode: String,
    val label: String,
    val contentMode: String, // "RECORD" or "TYPE"
    val textMessage: String = "",
    val audioFilePath: String? = null,
    val dueTimestamp: Long,
    val isCompleted: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

