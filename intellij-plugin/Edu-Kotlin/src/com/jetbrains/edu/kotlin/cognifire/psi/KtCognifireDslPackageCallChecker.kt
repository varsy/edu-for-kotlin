package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KtCognifireDslPackageCallChecker : CognifireDslPackageCallChecker {

  override fun isCallFromCognifireDslPackage(element: PsiElement) =
    element is KtCallExpression &&
    element.calleeExpression.getResolvedCall(element.analyze())?.resultingDescriptor?.containingDeclaration?.fqNameSafe == FqName(
      COGNIFIRE_DSL_PACKAGE
    )

  companion object {
    private const val COGNIFIRE_DSL_PACKAGE = "org.jetbrains.academy.cognifire.dsl"
  }
}
