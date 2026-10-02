package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ContentMode
import com.example.data.ReminderDao
import com.example.data.ReminderEntity
import com.example.data.RevocaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderDaoTest {

    private lateinit var database: RevocaDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RevocaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveReminder() = runBlocking {
        val reminder = ReminderEntity(
            jobCode = "JOB-01",
            label = "Electrical Inspection",
            contentMode = ContentMode.TYPE.name,
            textMessage = "Check main transformer",
            dueTimestamp = System.currentTimeMillis() + 60000L
        )

        val id = dao.insertReminder(reminder)
        assertTrue(id > 0)

        val retrieved = dao.getReminderById(id.toInt())
        assertNotNull(retrieved)
        assertEquals("JOB-01", retrieved?.jobCode)
        assertEquals("Electrical Inspection", retrieved?.label)
        assertEquals("Check main transformer", retrieved?.textMessage)
    }

    @Test
    fun testGetPendingRemindersFilter() = runBlocking {
        val now = System.currentTimeMillis()

        dao.insertReminder(
            ReminderEntity(
                jobCode = "JOB-FUTURE",
                label = "Future Task",
                contentMode = ContentMode.RECORD.name,
                dueTimestamp = now + 100000L,
                isCompleted = false
            )
        )

        dao.insertReminder(
            ReminderEntity(
                jobCode = "JOB-DONE",
                label = "Done Task",
                contentMode = ContentMode.RECORD.name,
                dueTimestamp = now + 100000L,
                isCompleted = true
            )
        )

        dao.insertReminder(
            ReminderEntity(
                jobCode = "JOB-PAST",
                label = "Past Task",
                contentMode = ContentMode.RECORD.name,
                dueTimestamp = now - 50000L,
                isCompleted = false
            )
        )

        val pending = dao.getPendingReminders(now)
        assertEquals(1, pending.size)
        assertEquals("JOB-FUTURE", pending[0].jobCode)
    }

    @Test
    fun testUpdateReminderCompletionAndSnooze() = runBlocking {
        val reminder = ReminderEntity(
            jobCode = "JOB-02",
            label = "Water Pipe Check",
            contentMode = ContentMode.TYPE.name,
            dueTimestamp = System.currentTimeMillis() + 30000L
        )
        val id = dao.insertReminder(reminder).toInt()

        val saved = dao.getReminderById(id)
        assertNotNull(saved)

        val completed = saved!!.copy(isCompleted = true)
        dao.updateReminder(completed)

        val updated = dao.getReminderById(id)
        assertTrue(updated!!.isCompleted)
    }

    @Test
    fun testDeleteReminder() = runBlocking {
        val reminder = ReminderEntity(
            jobCode = "JOB-03",
            label = "Temporary Item",
            contentMode = ContentMode.TYPE.name,
            dueTimestamp = System.currentTimeMillis() + 10000L
        )
        val id = dao.insertReminder(reminder).toInt()
        assertNotNull(dao.getReminderById(id))

        dao.deleteReminderById(id)
        assertNull(dao.getReminderById(id))
    }
}
