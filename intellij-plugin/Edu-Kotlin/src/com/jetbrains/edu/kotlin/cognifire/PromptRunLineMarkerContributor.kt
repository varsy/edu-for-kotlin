package com.jetbrains.edu.kotlin.cognifire

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import com.jetbrains.edu.cognifire.actions.PromptExecutorAction
import com.jetbrains.edu.cognifire.manager.PromptActionManager
import com.jetbrains.edu.cognifire.manager.PromptCodeState
import com.jetbrains.edu.cognifire.ui.CognifireIcons
import com.jetbrains.edu.cognifire.utils.PROMPT
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class PromptRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    val project = element.project
    val task = project.getCurrentTask() ?: return null
    val targetElement = element.parent.parent
    if (element is LeafPsiElement &&
        targetElement is KtCallExpression &&
        element.text == PROMPT && runReadAction {
          CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(element.parent.parent, element.language)
        }
    ) {
      val function = PsiTreeUtil.getParentOfType(targetElement, KtNamedFunction::class.java) ?: return null
      val uniqueId = "${function.fqName}:${function.valueParameters.size}"
      val action = PromptExecutorAction(targetElement, uniqueId, task)
      var tooltipText = EduKotlinBundle.message("action.run.generate.code.text")
      val promptActionManager = PromptActionManager.getInstance(project)
      val promptAction = promptActionManager.getAction(uniqueId)
      if (promptAction != null && promptAction.state != PromptCodeState.PromptWritten) {
        tooltipText = EduKotlinBundle.message("action.run.sync.prompt.with.code.text")
      } else {
        promptActionManager.addAction(uniqueId, task.id)
      }
      task.isPromptActionsGeneratedSuccessfully = promptActionManager.generatedSuccessfully(task.id)
      TaskToolWindowView.getInstance(project).updateCheckPanel(task)
      return Info(
        CognifireIcons.Sync,
        arrayOf(action),
      ) { _ -> tooltipText }
    }
    return null
  }
}
