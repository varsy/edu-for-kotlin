package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_SOLUTIONS_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import org.jetbrains.annotations.NonNls
import java.util.*

open class CompareWithAnswerAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val state = project.eduState ?: return

    val task = state.task
    val taskFile = state.taskFile

    if (task.course is HyperskillCourse) {
      val url = hyperskillTaskLink(task)
      EduBrowser.getInstance().browse("$url$HYPERSKILL_SOLUTIONS_ANCHOR")
      return
    }

    val taskFiles = getTaskFiles(task)
    putSelectedTaskFileFirst(taskFiles, taskFile)

    val solutionFilePaths = mutableListOf<String>()
    val requests = taskFiles.map {
      val virtualFile = it.getVirtualFile(state.project) ?: error("VirtualFile for ${it.name} not found")
      val studentFileContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
      val solution = it.getSolution()
      val solutionFileContent = DiffContentFactory.getInstance().create(solution, virtualFile.fileType)
      solutionFilePaths.add(virtualFile.path)
      SimpleDiffRequest(EduCoreBundle.message("action.Educational.CompareWithAnswer.description"), studentFileContent, solutionFileContent,
                        virtualFile.name,
                        EduCoreBundle.message("action.compare.answer", virtualFile.name))
    }
    if (requests.isEmpty()) {
      val message = JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder(EduCoreBundle.message("action.Educational.CompareWithAnswer.popup.content.no.solution.provided"), MessageType.INFO, null)
      message.createBalloon().show(JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext), Balloon.Position.above)
      return
    }
    val diffRequestChain = SimpleDiffRequestChain(requests)
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, solutionFilePaths)
    showSolution(state.project, diffRequestChain)
    EduCounterUsageCollector.solutionPeeked()
  }

  protected open fun showSolution(project: Project, diffRequestChain: SimpleDiffRequestChain) {
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  private fun getTaskFiles(task: Task) =
    task.taskFiles.values.filter { it.answerPlaceholders.isNotEmpty() }.toMutableList()

  private fun putSelectedTaskFileFirst(taskFiles: List<TaskFile>, selectedTaskFile: TaskFile) {
    val selectedTaskFileIndex = taskFiles.indexOf(selectedTaskFile)
    if (selectedTaskFileIndex > 0) {
      Collections.swap(taskFiles, 0, selectedTaskFileIndex)
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!project.isStudentProject()) {
      return
    }
    val task = project.getCurrentTask() ?: return

    presentation.isEnabledAndVisible = task.canShowSolution()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.CompareWithAnswer"
  }
}