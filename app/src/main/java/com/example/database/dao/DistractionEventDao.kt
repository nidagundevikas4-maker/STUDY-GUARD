package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.DistractionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractionEventDao {
  @Query("SELECT * FROM distraction_events ORDER BY timestampEpochMs DESC")
  fun observeAllEvents(): Flow<List<DistractionEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: DistractionEventEntity)

  @Query("DELETE FROM distraction_events")
  suspend fun clearAllEvents()
}
