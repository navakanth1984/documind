package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_subjects")
data class StudySubject(
    @PrimaryKey val id: String,
    val subjectName: String,
    val title: String,
    val description: String,
    val linkedDocumentId: String? = null,
    val hostRole: String = "Teacher", // "Teacher" or "Student"
    val participantCount: Int = 4,
    val timestamp: Long = System.currentTimeMillis()
)
