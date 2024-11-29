package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.parsers.GeneratedCodeParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.kotlin.cognifire.utils.createPsiFile
import com.jetbrains.edu.kotlin.cognifire.utils.getTodoMessageOrNull

class KtGeneratedCodeParser : GeneratedCodeParser {
  override fun hasErrors(project: Project, generatedCode: String, functionSignature: FunctionSignature): Boolean {
    val file = createPsiFile(project, functionSignature.toString(), generatedCode)
    val todoStrings = mutableListOf<String>()
    file.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        element.getTodoMessageOrNull()?.let { todoStrings.add(it) }
        super.visitElement(element)
      }
    })
    return todoStrings.isNotEmpty()
  }
}
