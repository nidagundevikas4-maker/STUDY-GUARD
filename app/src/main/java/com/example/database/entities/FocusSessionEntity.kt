package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val modeName: String,
  val plannedDurationMinutes: Int,
  val actualDurationSeconds: Int,
  val startedAtEpochMs: Long,
  val endedAtEpochMs: Long,
  val outcome: String,
  val dismissedNotificationsCount: Int = 0,
)
