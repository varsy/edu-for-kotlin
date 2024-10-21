package com.jetbrains.edu.cognifire.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

fun isCognifireApplicable(course: Course) = course.languageId == KOTLIN

fun PsiElement.isPromptBlock() = text.startsWith(PROMPT) &&
                                          CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(this, this.language)

const val PROMPT = "prompt"
const val CODE = "code"
const val UNIT_RETURN_VALUE = "Unit"

fun PromptToCodeResponse.toGeneratedCode() = distinctBy { it.codeLineNumber }.joinToString(System.lineSeparator()) { it.generatedCodeLine }
