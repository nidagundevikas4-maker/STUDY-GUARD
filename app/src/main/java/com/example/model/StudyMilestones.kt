package com.example.model

data class StudyLevelInfo(
  val levelNumber: Int,
  val title: String,
  val nextLevelTitle: String?,
  val totalCompletedMinutes: Int,
  val levelStartMinutes: Int,
  val nextLevelMinutes: Int?,
) {
  val progressToNextLevel: Float
    get() {
      val next = nextLevelMinutes ?: return 1f
      val span = (next - levelStartMinutes).coerceAtLeast(1)
      return ((totalCompletedMinutes - levelStartMinutes).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

  val minutesToNextLevel: Int
    get() = nextLevelMinutes?.let { (it - totalCompletedMinutes).coerceAtLeast(0) } ?: 0
}

enum class MilestoneBadgeId {
  FIRST_FOCUS,
  TEN_HOURS,
  STRICT_STREAK_7,
  ZERO_DISTRACTION_WEEK,
  DEEP_SCHOLAR_5,
  SHIELD_GUARDIAN,
}

data class MilestoneBadge(
  val id: MilestoneBadgeId,
  val title: String,
  val description: String,
  val isUnlocked: Boolean,
  val currentProgress: Int,
  val targetProgress: Int,
  val progressText: String,
) {
  val progressFraction: Float
    get() =
      if (targetProgress <= 0) 0f
      else (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f)
}

data class StudyMilestoneSummary(
  val level: StudyLevelInfo,
  val badges: List<MilestoneBadge>,
) {
  val unlockedCount: Int
    get() = badges.count { it.isUnlocked }
}
