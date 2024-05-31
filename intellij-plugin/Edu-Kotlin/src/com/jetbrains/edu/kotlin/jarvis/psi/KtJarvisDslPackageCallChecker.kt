package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KtJarvisDslPackageCallChecker : JarvisDslPackageCallChecker {

  override fun isCallFromJarvisDslPackage(element: PsiElement): Boolean {
    if (element is KtCallExpression) {
      return Companion.isCallFromJarvisDslPackage(element)
    } else {
      error("The element must be a call expression")
    }
  }

  companion object {
    private const val JARVIS_DSL_PACKAGE = "org.jetbrains.academy.jarvis.dsl"

    fun isCallFromJarvisDslPackage(callExpression: KtCallExpression) =
      callExpression.calleeExpression.getResolvedCall(callExpression.analyze())?.resultingDescriptor?.containingDeclaration?.fqNameSafe == FqName(JARVIS_DSL_PACKAGE)
  }
}
