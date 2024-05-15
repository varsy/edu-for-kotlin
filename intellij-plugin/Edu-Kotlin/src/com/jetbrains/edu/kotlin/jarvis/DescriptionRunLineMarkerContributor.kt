package com.jetbrains.edu.kotlin.jarvis

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.edu.jarvis.actions.DescriptionExecutorAction
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DescriptionRunLineMarkerContributor : RunLineMarkerContributor(), DumbAware {
  override fun getInfo(element: PsiElement): Info? {
    if (element is LeafPsiElement &&
        element.parent.parent is KtCallExpression &&
        element.text == DESCRIPTION
//        isCallFromJarvisDslPackage(element.parent.parent as KtCallExpression)
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
  companion object {
    const val DESCRIPTION = "description"
    private const val JARVIS_DSL_PACKAGE = "org.jetbrains.academy.jarvis.dsl"

    fun isCallFromJarvisDslPackage(callExpression: KtCallExpression) =
      callExpression.calleeExpression.getResolvedCall(callExpression.analyze())?.resultingDescriptor?.containingDeclaration?.fqNameSafe == FqName(JARVIS_DSL_PACKAGE)

  }
}
