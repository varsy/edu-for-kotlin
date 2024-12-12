package com.jetbrains.edu.cognifire.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import com.intellij.openapi.application.runReadAction

fun isCognifireApplicable(course: Course) = course.languageId == KOTLIN

fun PsiElement.isPromptBlock() = runReadAction {
  text.startsWith(PROMPT) && CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(this, this.language)
}

const val PROMPT = "prompt"
const val CODE = "code"
const val UNIT_RETURN_VALUE = "Unit"

fun PromptToCodeContent.toGeneratedCode() =
  distinctBy { it.codeLineNumber }.joinToString(System.lineSeparator()) { it.generatedCodeLine }

fun PromptToCodeContent.toPrompt() =
  distinctBy { it.promptLineNumber }.joinToString(System.lineSeparator()) { it.promptLine }.prependIndent()
