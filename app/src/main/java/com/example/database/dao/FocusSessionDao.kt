package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
  @Query("SELECT * FROM focus_sessions ORDER BY endedAtEpochMs DESC")
  fun observeAllSessions(): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions WHERE endedAtEpochMs >= :startOfDayEpochMs ORDER BY endedAtEpochMs DESC")
  fun observeSessionsSince(startOfDayEpochMs: Long): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions WHERE endedAtEpochMs >= :startOfDayEpochMs ORDER BY endedAtEpochMs DESC")
  suspend fun getSessionsSince(startOfDayEpochMs: Long): List<FocusSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: FocusSessionEntity): Long

  @Query("DELETE FROM focus_sessions WHERE id = :id")
  suspend fun deleteSessionById(id: Long)

  @Query("DELETE FROM focus_sessions")
  suspend fun clearAllSessions()
}
