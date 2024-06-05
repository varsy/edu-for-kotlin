package com.jetbrains.edu.kotlin.jarvis

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.jarvis.actions.DescriptionExecutorAction
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import org.jetbrains.kotlin.psi.KtCallExpression

class DescriptionRunLineMarkerContributor : RunLineMarkerContributor(), DumbAware {
  override fun getInfo(element: PsiElement): Info? {
    if (element is LeafPsiElement &&
        element.parent.parent is KtCallExpression &&
        element.text == DESCRIPTION &&
        JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(element.parent.parent, element.language)
      ) {
      return Info(
        AllIcons.Actions.Execute,
        arrayOf(DescriptionExecutorAction(element.parent.parent)),
      ) { _ ->
        EduKotlinBundle.message("action.run.generate.code.text")
      }
    }
    return null
  }
}
