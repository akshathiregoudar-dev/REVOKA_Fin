package com.example.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun getCount(): Int = reminderDao.getCount()

    suspend fun insert(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)

    suspend fun update(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)

    suspend fun deleteById(id: Int) = reminderDao.deleteReminderById(id)

    suspend fun getById(id: Int): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun getPendingReminders(currentTime: Long): List<ReminderEntity> = reminderDao.getPendingReminders(currentTime)

    suspend fun clearAll() = reminderDao.deleteAll()
}
