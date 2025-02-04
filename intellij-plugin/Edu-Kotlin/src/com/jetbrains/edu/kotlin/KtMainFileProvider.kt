package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.MainFileProvider
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtMainFileProvider : MainFileProvider {
  override fun findMainClassName(project: Project, file: VirtualFile): String? {
    val mainFunction = findMainPsi(project, file) ?: return null
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(mainFunction) ?: return null
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }

  override fun findMainPsi(project: Project, file: VirtualFile): PsiElement? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null

    val mainFunctionDetector = KotlinMainFunctionDetector.getInstanceDumbAware(project)
    return PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java).find {
      val function = it as? KtNamedFunction ?: return@find false
      mainFunctionDetector.isMain(function)
    }
  }
}
