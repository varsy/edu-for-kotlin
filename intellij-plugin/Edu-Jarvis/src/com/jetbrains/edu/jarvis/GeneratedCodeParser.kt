package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Parses the generated code and returns a list of errors (error description inside TODO blocks)
 */
interface GeneratedCodeParser {
  fun parseGeneratedCode(project: Project, generatedCode: String): List<String>

  companion object {
    private val EP_NAME = LanguageExtension<GeneratedCodeParser>("Educational.generatedCodeParser")

    fun parseGeneratedCode(project: Project, generatedCode: String, language: Language): List<String> {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language).parseGeneratedCode(project, generatedCode)
    }
  }
}