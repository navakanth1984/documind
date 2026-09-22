package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_chat_messages")
data class StudyChatMessage(
    @PrimaryKey val id: String,
    val subjectId: String,
    val senderName: String,
    val senderRole: String, // "Student", "Teacher", "AI Tutor"
    val message: String,
    val thinkingText: String? = null,
    val searchSources: String? = null,
    val audioBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
