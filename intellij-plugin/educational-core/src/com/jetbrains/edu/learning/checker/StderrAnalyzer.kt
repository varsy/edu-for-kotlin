package com.jetbrains.edu.learning.checker

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.jetbrains.edu.learning.courseFormat.CheckResult

interface StderrAnalyzer {
  fun tryToGetCheckResult(stderr: String): CheckResult? = null

  fun getStackTrace(stderr: String): List<Pair<String, Int>> = emptyList()

  companion object {
    val EP_NAME = LanguageExtension<StderrAnalyzer>("Educational.stderrAnalyzer")

    fun getInstance(language: Language): StderrAnalyzer? = EP_NAME.forLanguage(language)
  }
}