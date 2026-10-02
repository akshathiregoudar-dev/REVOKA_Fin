package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class RevocaDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: RevocaDatabase? = null

        fun getDatabase(context: Context): RevocaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RevocaDatabase::class.java,
                    "revoca_hub_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
