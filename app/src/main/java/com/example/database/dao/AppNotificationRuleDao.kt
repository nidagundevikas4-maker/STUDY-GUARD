package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.AppNotificationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationRuleDao {
  @Query("SELECT * FROM app_notification_rules WHERE focusMode = :focusMode ORDER BY appLabel ASC")
  fun observeRulesForMode(focusMode: String): Flow<List<AppNotificationRuleEntity>>

  @Query("SELECT * FROM app_notification_rules WHERE packageName = :packageName AND focusMode = :focusMode LIMIT 1")
  suspend fun getRule(packageName: String, focusMode: String): AppNotificationRuleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRule(rule: AppNotificationRuleEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertRulesIfAbsent(rules: List<AppNotificationRuleEntity>)

  @Query("DELETE FROM app_notification_rules WHERE focusMode = :focusMode")
  suspend fun clearRulesForMode(focusMode: String)

  @Query("DELETE FROM app_notification_rules")
  suspend fun clearAllRules()
}
