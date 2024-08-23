package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.jarvis.GeneratedCodeParser
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtCallExpression

class KtGeneratedCodeParser : GeneratedCodeParser {
  override fun hasErrors(project: Project, generatedCode: String): Boolean {
    val psiFactory = PsiFileFactory.getInstance(project)
    val file = psiFactory.createFileFromText("fileName.kt", KotlinLanguage.INSTANCE, generatedCode)
    val todoStrings = mutableListOf<String>()
    file.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (element is KtCallExpression && element.calleeExpression?.text == TODO_MARKER) {
          todoStrings.add(element.valueArguments.firstOrNull()?.getArgumentExpression()?.text ?: EMPTY_TODO)
        } else if (element.text == TODO_MARKER) {
          todoStrings.add(EMPTY_TODO)
        }
        super.visitElement(element)
      }
    })
    return todoStrings.isNotEmpty()
  }

  companion object {
    private const val TODO_MARKER = "TODO"
    private const val EMPTY_TODO = ""
  }
}
