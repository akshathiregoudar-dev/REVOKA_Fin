package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueTimestamp <= :currentTime")
    suspend fun getMissedReminders(currentTime: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueTimestamp ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueTimestamp > :currentTime")
    suspend fun getPendingReminders(currentTime: Long): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
