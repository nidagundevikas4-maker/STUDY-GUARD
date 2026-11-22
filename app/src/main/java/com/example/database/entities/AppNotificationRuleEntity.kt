package com.example.database.entities

import androidx.room.Entity

@Entity(
  tableName = "app_notification_rules",
  primaryKeys = ["packageName", "focusMode"],
)
data class AppNotificationRuleEntity(
  val packageName: String,
  val focusMode: String,
  val appLabel: String,
  val category: String,
  val action: String,
  val isUserCustomized: Boolean = false,
)
