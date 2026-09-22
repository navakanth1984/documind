package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudySubject
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_subjects ORDER BY timestamp DESC")
    fun getAllSubjects(): Flow<List<StudySubject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: StudySubject)

    @Query("SELECT * FROM study_chat_messages WHERE subjectId = :subjectId ORDER BY timestamp ASC")
    fun getMessagesForSubject(subjectId: String): Flow<List<StudyChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: StudyChatMessage)

    @Query("DELETE FROM study_subjects WHERE id = :id")
    suspend fun deleteSubject(id: String)
}
