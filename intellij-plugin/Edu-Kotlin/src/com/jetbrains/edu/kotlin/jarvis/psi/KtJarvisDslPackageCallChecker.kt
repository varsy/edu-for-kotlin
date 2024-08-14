package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private const val JARVIS_DSL_PACKAGE = "org.jetbrains.academy.jarvis.dsl"

class KtJarvisDslPackageCallChecker : JarvisDslPackageCallChecker {

  override fun isCallFromJarvisDslPackage(element: PsiElement) =
    element is KtCallExpression &&
    element.calleeExpression.getResolvedCall(element.analyze())?.resultingDescriptor?.containingDeclaration?.fqNameSafe == FqName(JARVIS_DSL_PACKAGE)
}
