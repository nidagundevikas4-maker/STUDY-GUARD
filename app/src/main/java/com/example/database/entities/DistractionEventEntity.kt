package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distraction_events")
data class DistractionEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val sessionId: Long? = null,
  val packageName: String,
  val appLabel: String,
  val focusMode: String,
  val timestampEpochMs: Long,
  val outcome: String,
)
