package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentMetadataEntity
import com.example.data.model.OcrRecordEntity
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudySubject

@Database(
    entities = [
        DocumentEntity::class,
        OcrRecordEntity::class,
        DocumentMetadataEntity::class,
        StudySubject::class,
        StudyChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DocuMindDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun ocrRecordDao(): OcrRecordDao
    abstract fun documentMetadataDao(): DocumentMetadataDao
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: DocuMindDatabase? = null

        fun getDatabase(context: Context): DocuMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocuMindDatabase::class.java,
                    "documind_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

