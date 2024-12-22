package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.CourseViewUtils.modifyTaskChildNode

open class DirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  task: Task?
) : EduNode<Task>(project, value, viewSettings, context, task) {

  override fun canNavigate(): Boolean = true

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return modifyTaskChildNode(myProject, childNode, item, this::createChildFileNode, this::createChildDirectoryNode)
  }

  open fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return DirectoryNode(myProject, value, settings, context, item)
  }

  open fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): AbstractTreeNode<*> {
    return originalNode
  }

  override fun updateImpl(data: PresentationData) {
    val course = StudyTaskManager.getInstance(myProject).course ?: return
    val dir = value
    val directoryFile = dir.virtualFile
    val name = directoryFile.name
    if (name == course.sourceDir || course.testDirs.contains(name)) {
      data.presentableText = name
    }
    else {
      val parentValue = parentValue
      data.presentableText = ProjectViewDirectoryHelper.getInstance(myProject).getNodeName(settings, parentValue, dir)
    }
  }
}
