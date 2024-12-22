@file:JvmName("CCCourseViewUtil")

package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.coursecreator.actions.taskFile.CCIgnoreFileInSyncChanges
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseViewContext
import com.jetbrains.edu.learning.projectView.RootNode
import com.jetbrains.edu.learning.projectView.TaskNode
import javax.swing.tree.TreeNode


fun modifyNodeInEducatorMode(project: Project, viewSettings: ViewSettings, context: CourseViewContext, childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
  val value = childNode.value
  return when (value) {
    is PsiDirectory -> CCNode(project, value, viewSettings, context, null)
    is PsiFile -> CCStudentInvisibleFileNode(project, value, viewSettings, context)
    else -> null
  }
}

fun findAncestorTaskNode(node: AbstractTreeNode<*>): TaskNode? {
  var currentNode = node
  while (currentNode !is TaskNode && currentNode !is RootNode) {
    currentNode = currentNode.parent
  }
  return currentNode as? TaskNode
}

fun isNodeInFrameworkLessonTask(node: PsiFileNode): Boolean {
  // find the task using parent nodes, because finding the task using VFS in mouse adapter will be slower
  val taskNode = findAncestorTaskNode(node)
  val task = taskNode?.item
  return task?.lesson is FrameworkLesson
}

fun SyncChangesHelpTooltip.tryInstallNewTooltip(project: Project, treeNode: TreeNode): Boolean {
  val node = TreeUtil.getUserObject(treeNode) as? CCFileNode ?: return false
  if (!isNodeInFrameworkLessonTask(node)) return false

  val taskFile = node.virtualFile?.getTaskFile(project) ?: return false

  var title: String? = null
  var description: String? = null
  var actionText: String? = null
  var secondaryActionText: String? = null

  val state = SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile)

  when (state) {
    null -> return false
    SyncChangesTaskFileState.INFO -> {
      title = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.Changes.text")
      description = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.Changes.description")
      actionText = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.Changes.text")
      secondaryActionText = EduCoreBundle.message("action.Educational.Educator.IgnoreFilePropagation.ActionLink.text")
    }

    SyncChangesTaskFileState.WARNING -> {
      title = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.File.text")
      description = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.File.description")
      actionText = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.File.text")
      secondaryActionText = EduCoreBundle.message("action.Educational.Educator.IgnoreFilePropagation.ActionLink.File.text")
    }
  }

  clearLinks()

  setTitle(title)
  setDescription(description)
  addLink(actionText) {
    CCFrameworkLessonManager.getInstance(project).propagateChanges(taskFile.task, listOf(taskFile))
  }
  addLink(secondaryActionText) {
    CCIgnoreFileInSyncChanges.runWithTaskFile(project, taskFile)
  }
  setLocation(SyncChangesHelpTooltip.Alignment.EXACT_CURSOR)
  return true
}