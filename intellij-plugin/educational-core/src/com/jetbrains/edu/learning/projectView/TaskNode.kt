package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask
import com.jetbrains.edu.learning.projectView.CourseViewUtils.modifyTaskChildNode

open class TaskNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  task: Task
) : EduNode<Task>(project, value, viewSettings, context, task) {

  override val item: Task get() = super.item!!

  override fun getWeight(): Int = item.index
  override fun expandOnDoubleClick(): Boolean = false
  override fun canNavigate(): Boolean = true

  override fun navigate(requestFocus: Boolean) {
    navigateToTask(myProject, item)
  }

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return modifyTaskChildNode(myProject, childNode, item, this::createChildFileNode, this::createChildDirectoryNode)
  }

  open fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return DirectoryNode(myProject, value, settings, context, item)
  }

  open fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): AbstractTreeNode<*> {
    return originalNode
  }
}
