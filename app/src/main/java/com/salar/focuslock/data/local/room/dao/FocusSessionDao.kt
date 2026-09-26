package com.salar.focuslock.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.salar.focuslock.data.local.room.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query(
        "SELECT * FROM focus_sessions WHERE startedAt >= :fromEpochMillis " +
            "AND startedAt < :toEpochMillis ORDER BY startedAt DESC"
    )
    suspend fun getSessionsBetween(fromEpochMillis: Long, toEpochMillis: Long): List<FocusSessionEntity>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Delete
    suspend fun delete(session: FocusSessionEntity)
}
