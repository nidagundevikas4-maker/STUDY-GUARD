package com.example

import com.example.model.AcademicAppClassifier
import com.example.model.AppCategory
import com.example.model.FocusMode
import com.example.model.RuleAction
import org.junit.Assert.assertEquals
import org.junit.Test

class AcademicAppClassifierTest {

  @Test
  fun `classifies student LMS calendar and email apps as academic`() {
    assertEquals(
      AppCategory.ACADEMIC,
      AcademicAppClassifier.classify("com.google.android.apps.classroom", "Classroom"),
    )
    assertEquals(
      AppCategory.ACADEMIC,
      AcademicAppClassifier.classify("com.instructure.candroid", "Canvas Student"),
    )
    assertEquals(
      AppCategory.ACADEMIC,
      AcademicAppClassifier.classify("com.google.android.gm", "Gmail"),
    )
    assertEquals(
      AppCategory.ACADEMIC,
      AcademicAppClassifier.classify("com.google.android.calendar", "Calendar"),
    )
    assertEquals(
      AppCategory.ACADEMIC,
      AcademicAppClassifier.classify("us.zoom.videomeetings", "Zoom"),
    )
  }

  @Test
  fun `classifies social entertainment and messaging apps accurately`() {
    assertEquals(
      AppCategory.SOCIAL,
      AcademicAppClassifier.classify("com.instagram.android", "Instagram"),
    )
    assertEquals(
      AppCategory.ENTERTAINMENT,
      AcademicAppClassifier.classify("com.google.android.youtube", "YouTube"),
    )
    assertEquals(
      AppCategory.COMMUNICATION,
      AcademicAppClassifier.classify("com.whatsapp", "WhatsApp"),
    )
    assertEquals(
      AppCategory.OTHER,
      AcademicAppClassifier.classify("com.example.calculator", "Calculator"),
    )
  }

  @Test
  fun `applies expected default rule policies across focus modes`() {
    assertEquals(
      RuleAction.ALLOW,
      AcademicAppClassifier.defaultActionFor(AppCategory.ACADEMIC, FocusMode.DEEP_FOCUS),
    )
    assertEquals(
      RuleAction.SILENCE,
      AcademicAppClassifier.defaultActionFor(AppCategory.COMMUNICATION, FocusMode.DEEP_FOCUS),
    )
    assertEquals(
      RuleAction.SILENCE,
      AcademicAppClassifier.defaultActionFor(AppCategory.SOCIAL, FocusMode.DEEP_FOCUS),
    )

    assertEquals(
      RuleAction.ALLOW,
      AcademicAppClassifier.defaultActionFor(AppCategory.ACADEMIC, FocusMode.STUDY_MODE),
    )
    assertEquals(
      RuleAction.ALLOW,
      AcademicAppClassifier.defaultActionFor(AppCategory.OTHER, FocusMode.STUDY_MODE),
    )
    assertEquals(
      RuleAction.SILENCE,
      AcademicAppClassifier.defaultActionFor(AppCategory.COMMUNICATION, FocusMode.STUDY_MODE),
    )

    assertEquals(
      RuleAction.ALLOW,
      AcademicAppClassifier.defaultActionFor(AppCategory.COMMUNICATION, FocusMode.LIGHT_FOCUS),
    )
    assertEquals(
      RuleAction.SILENCE,
      AcademicAppClassifier.defaultActionFor(AppCategory.SOCIAL, FocusMode.LIGHT_FOCUS),
    )
    assertEquals(
      RuleAction.SILENCE,
      AcademicAppClassifier.defaultActionFor(AppCategory.ENTERTAINMENT, FocusMode.LIGHT_FOCUS),
    )
  }
}
