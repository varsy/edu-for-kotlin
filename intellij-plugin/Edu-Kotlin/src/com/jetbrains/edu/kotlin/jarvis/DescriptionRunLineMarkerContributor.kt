package com.jetbrains.edu.kotlin.jarvis

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.jarvis.actions.DescriptionExecutorAction
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class DescriptionRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    val project = element.project
    val task = project.getCurrentTask() ?: return null
    val targetElement = element.parent.parent
    if (element is LeafPsiElement &&
        targetElement is KtCallExpression &&
        element.text == DESCRIPTION &&
        JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(element.parent.parent, element.language)
      ) {
      val function = PsiTreeUtil.getParentOfType(targetElement, KtNamedFunction::class.java) ?: return null
      val uniqueId = "${targetElement.containingFile.name}:${function.name}"
      val action = DescriptionExecutorAction(targetElement, uniqueId)
      task.promptActionManager.addAction(uniqueId)
      TaskToolWindowView.getInstance(project).updateCheckPanel(task)
      return Info(
        AllIcons.Actions.Execute,
        arrayOf(action),
      ) { _ ->
        EduKotlinBundle.message("action.run.generate.code.text")
      }
    }
    return null
  }
}
