package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.database.dao.AppNotificationRuleDao
import com.example.database.dao.DistractionEventDao
import com.example.database.dao.FocusSessionDao
import com.example.database.entities.AppNotificationRuleEntity
import com.example.database.entities.DistractionEventEntity
import com.example.database.entities.FocusSessionEntity

@Database(
  entities = [
    FocusSessionEntity::class,
    AppNotificationRuleEntity::class,
    DistractionEventEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class FocusFlowDatabase : RoomDatabase() {
  abstract fun focusSessionDao(): FocusSessionDao

  abstract fun appNotificationRuleDao(): AppNotificationRuleDao

  abstract fun distractionEventDao(): DistractionEventDao

  companion object {
    @Volatile
    private var INSTANCE: FocusFlowDatabase? = null

    fun getInstance(context: Context): FocusFlowDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                FocusFlowDatabase::class.java,
                "focusflow_local.db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
