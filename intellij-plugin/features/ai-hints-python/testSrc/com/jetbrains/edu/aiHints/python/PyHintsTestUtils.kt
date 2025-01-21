package com.jetbrains.edu.aiHints.python

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.PythonFileType

object PyHintsTestUtils {
  fun createPsiFile(project: Project, code: String): PsiFile {
    return PsiFileFactory.getInstance(project).createFileFromText("task.py", PythonFileType.INSTANCE, code)
  }
}