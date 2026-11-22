package com.example.model

enum class AppCategory(val label: String) {
  ACADEMIC("Academic"),
  COMMUNICATION("Communication"),
  SOCIAL("Social"),
  ENTERTAINMENT("Entertainment"),
  OTHER("Other"),
}

enum class RuleAction(val label: String) {
  ALLOW("Allow"),
  SILENCE("Silence"),
}

data class DistractionEventRecord(
  val id: Long,
  val sessionId: Long?,
  val packageName: String,
  val appLabel: String,
  val focusMode: FocusMode,
  val timestampEpochMs: Long,
  val outcome: String,
)

object AcademicAppClassifier {

  private val academicKeywords =
    listOf(
      "classroom",
      "instructure",
      "canvas",
      "blackboard",
      "moodle",
      "brightspace",
      "schoology",
      "piazza",
      "edmodo",
      "coursera",
      "edx",
      "khanacademy",
      "quizlet",
      "anki",
      "notion",
      "obsidian",
      "onenote",
      "docs",
      "drive",
      "calendar",
      "gmail",
      "outlook",
      "spark",
      "protonmail",
      "teams",
      "zoom",
      "meet",
      "webex",
      "slack",
      "remind",
      "gradescope",
      "turnitin",
      "duolingo",
      "wolfram",
      "desmos",
      "scholar",
    )

  private val socialKeywords =
    listOf(
      "instagram",
      "tiktok",
      "musically",
      "snapchat",
      "twitter",
      "x.android",
      "facebook",
      "reddit",
      "threads",
      "pinterest",
      "tumblr",
      "bereal",
      "discord",
    )

  private val entertainmentKeywords =
    listOf(
      "youtube",
      "netflix",
      "twitch",
      "spotify",
      "hulu",
      "disney",
      "primevideo",
      "crunchyroll",
      "steam",
      "roblox",
      "supercell",
      "epicgames",
      "pubg",
      "genshin",
    )

  private val communicationKeywords =
    listOf(
      "whatsapp",
      "messaging",
      "mms",
      "telegram",
      "signal",
      "viber",
      "line",
      "wechat",
      "dialer",
      "contacts",
    )

  fun classify(packageName: String, appLabel: String = ""): AppCategory {
    val haystack = "${packageName.lowercase()} ${appLabel.lowercase()}"
    return when {
      academicKeywords.any { haystack.contains(it) } -> AppCategory.ACADEMIC
      socialKeywords.any { haystack.contains(it) } -> AppCategory.SOCIAL
      entertainmentKeywords.any { haystack.contains(it) } -> AppCategory.ENTERTAINMENT
      communicationKeywords.any { haystack.contains(it) } -> AppCategory.COMMUNICATION
      else -> AppCategory.OTHER
    }
  }

  fun defaultActionFor(category: AppCategory, mode: FocusMode): RuleAction =
    when (mode) {
      FocusMode.DEEP_FOCUS ->
        when (category) {
          AppCategory.ACADEMIC -> RuleAction.ALLOW
          else -> RuleAction.SILENCE
        }

      FocusMode.STUDY_MODE ->
        when (category) {
          AppCategory.ACADEMIC, AppCategory.OTHER -> RuleAction.ALLOW
          AppCategory.COMMUNICATION, AppCategory.SOCIAL, AppCategory.ENTERTAINMENT ->
            RuleAction.SILENCE
        }

      FocusMode.LIGHT_FOCUS ->
        when (category) {
          AppCategory.SOCIAL, AppCategory.ENTERTAINMENT -> RuleAction.SILENCE
          else -> RuleAction.ALLOW
        }
    }
}
