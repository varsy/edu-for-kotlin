package com.jetbrains.edu.kotlin.learning.eduAssistant.psi.context

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.eduAssistant.context.differ.FilesDiffer
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFilesDiffer : FilesDiffer {

  override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> {
    val beforeMethods = PsiTreeUtil.findChildrenOfType(before, KtNamedFunction::class.java).associateBy { it.fqName }
    val afterMethods = PsiTreeUtil.findChildrenOfType(after, KtNamedFunction::class.java).associateBy { it.fqName }

    return afterMethods.values.filterNot { method ->
      val matchingMethod = beforeMethods[method.fqName]
      matchingMethod != null && method.bodyExpression?.text == matchingMethod.bodyExpression?.text &&
      ((considerParameters && method.valueParameterList?.text == matchingMethod.valueParameterList?.text) || !considerParameters)
    }.mapNotNull { it.name }.toList()
  }
}
