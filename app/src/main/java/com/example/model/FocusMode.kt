package com.example.model

enum class FocusMode(
  val title: String,
  val subtitle: String,
  val policySummary: String,
  val allowedRuleLabel: String,
  val silencedRuleLabel: String,
  val defaultDurationMinutes: Int,
) {
  DEEP_FOCUS(
    title = "Deep Focus",
    subtitle = "Strict distraction shield",
    policySummary = "Blocks distracting apps, dismisses non-essential alerts, and enables priority DND.",
    allowedRuleLabel = "Approved contacts & alarms",
    silencedRuleLabel = "All distracting & social apps",
    defaultDurationMinutes = 45,
  ),
  STUDY_MODE(
    title = "Study Mode",
    subtitle = "Academics allowed",
    policySummary = "Allows Classroom, Canvas, Calendar, and email while blocking social & entertainment apps.",
    allowedRuleLabel = "Academic, LMS & Calendar",
    silencedRuleLabel = "Social & entertainment",
    defaultDurationMinutes = 25,
  ),
  LIGHT_FOCUS(
    title = "Light Focus",
    subtitle = "Gentle boundary",
    policySummary = "Silences only your selected high-distraction apps with flexible rules.",
    allowedRuleLabel = "Most apps & academic alerts",
    silencedRuleLabel = "Selected distraction apps",
    defaultDurationMinutes = 20,
  ),
}
