package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY dateIso ASC, startTime ASC, createdAt DESC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY dateIso ASC, startTime ASC, createdAt DESC")
    suspend fun getAllEventsList(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE googleCalendarEventId = :googleId LIMIT 1")
    suspend fun findByGoogleEventId(googleId: Long): CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>)

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE workspaceId = :workspaceId")
    suspend fun deleteEventsByWorkspace(workspaceId: String)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}
