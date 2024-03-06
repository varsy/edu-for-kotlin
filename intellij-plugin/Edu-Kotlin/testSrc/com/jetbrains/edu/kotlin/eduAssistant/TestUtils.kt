package com.jetbrains.edu.kotlin.eduAssistant

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.KotlinLanguage

val language = KotlinLanguage.INSTANCE

fun String.reformatCode(project: Project): String {
  val psi = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("file", language, this) }
  WriteCommandAction.runWriteCommandAction(project) {
    CodeStyleManager.getInstance(project).reformat(psi)
  }
  return runReadAction { psi.text }
}
