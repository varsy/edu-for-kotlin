package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Parses the generated code and returns a list of errors (error description inside TODO blocks)
 */
interface GeneratedCodeParser {
  fun hasErrors(project: Project, generatedCode: String): Boolean

  companion object {
    private val EP_NAME = LanguageExtension<GeneratedCodeParser>("Educational.generatedCodeParser")

    fun hasErrors(project: Project, generatedCode: String, language: Language): Boolean {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language).hasErrors(project, generatedCode)
    }
  }
}