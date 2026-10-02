package com.example.ai

data class ParsedReminder(
    val label: String,
    val spokenMessage: String,
    val dueEpochMs: Long,
    val dateDescription: String = ""
)
