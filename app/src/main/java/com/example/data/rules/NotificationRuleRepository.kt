package com.example.data.rules

import com.example.database.dao.AppNotificationRuleDao
import com.example.database.dao.DistractionEventDao
import com.example.database.entities.AppNotificationRuleEntity
import com.example.database.entities.DistractionEventEntity
import com.example.model.AcademicAppClassifier
import com.example.model.AppCategory
import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.RuleAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ResolvedRule(
  val action: RuleAction,
  val category: AppCategory,
  val isUserCustomized: Boolean,
)

class NotificationRuleRepository(
  private val ruleDao: AppNotificationRuleDao,
  private val distractionEventDao: DistractionEventDao,
) {

  fun observeRulesForMode(focusMode: FocusMode): Flow<Map<String, AppNotificationRuleEntity>> =
    ruleDao.observeRulesForMode(focusMode.name).map { list ->
      list.associateBy { it.packageName }
    }

  fun observeRecentDistractions(): Flow<List<DistractionEventRecord>> =
    distractionEventDao.observeAllEvents().map { events ->
      events.map { entity ->
        DistractionEventRecord(
          id = entity.id,
          sessionId = entity.sessionId,
          packageName = entity.packageName,
          appLabel = entity.appLabel,
          focusMode = FocusMode.entries.firstOrNull { it.name == entity.focusMode }
            ?: FocusMode.STUDY_MODE,
          timestampEpochMs = entity.timestampEpochMs,
          outcome = entity.outcome,
        )
      }
    }

  suspend fun resolveEffectiveRule(
    packageName: String,
    appLabel: String,
    focusMode: FocusMode,
    academicPriorityEnabled: Boolean,
  ): ResolvedRule {
    val category = AcademicAppClassifier.classify(packageName, appLabel)
    val savedRule = ruleDao.getRule(packageName = packageName, focusMode = focusMode.name)
    if (savedRule != null) {
      val parsedAction =
        RuleAction.entries.firstOrNull { it.name == savedRule.action } ?: RuleAction.ALLOW
      return ResolvedRule(
        action = parsedAction,
        category = category,
        isUserCustomized = savedRule.isUserCustomized,
      )
    }

    val defaultAction =
      if (category == AppCategory.ACADEMIC && academicPriorityEnabled) {
        RuleAction.ALLOW
      } else if (category == AppCategory.ACADEMIC && !academicPriorityEnabled && focusMode == FocusMode.DEEP_FOCUS) {
        RuleAction.SILENCE
      } else {
        AcademicAppClassifier.defaultActionFor(category, focusMode)
      }

    return ResolvedRule(
      action = defaultAction,
      category = category,
      isUserCustomized = false,
    )
  }

  suspend fun saveRule(
    packageName: String,
    appLabel: String,
    focusMode: FocusMode,
    category: AppCategory,
    action: RuleAction,
  ) {
    ruleDao.upsertRule(
      AppNotificationRuleEntity(
        packageName = packageName,
        focusMode = focusMode.name,
        appLabel = appLabel,
        category = category.name,
        action = action.name,
        isUserCustomized = true,
      )
    )
  }

  suspend fun resetRulesForMode(focusMode: FocusMode) {
    ruleDao.clearRulesForMode(focusMode.name)
  }

  suspend fun resetAllRules() {
    ruleDao.clearAllRules()
  }

  suspend fun logDistractionEvent(
    packageName: String,
    appLabel: String,
    focusMode: FocusMode,
    outcome: String,
    sessionId: Long? = null,
  ) {
    distractionEventDao.insertEvent(
      DistractionEventEntity(
        sessionId = sessionId,
        packageName = packageName,
        appLabel = appLabel,
        focusMode = focusMode.name,
        timestampEpochMs = System.currentTimeMillis(),
        outcome = outcome,
      )
    )
  }
}
