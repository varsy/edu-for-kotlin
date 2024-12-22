package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.learning.projectView.CourseViewContext
import com.jetbrains.edu.learning.projectView.TaskNode

class CCTaskNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  task: Task
) : TaskNode(project, value, viewSettings, context, task) {

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) {
      return node
    }
    val value = childNode.value
    if (value is PsiDirectory) {
      val name = value.name
      if (EduNames.BUILD == name || EduNames.OUT == name) return null
      return createChildDirectoryNode(value)
    }
    else if (value is PsiElement) {
      val psiFile = value.containingFile
      val virtualFile = psiFile.virtualFile ?: return null
      val course = StudyTaskManager.getInstance(myProject).course ?: return null
      val configurator = course.configurator ?: return CCStudentInvisibleFileNode(myProject, psiFile, settings, context)
      return if (!virtualFile.isTestsFile(myProject)) {
        CCStudentInvisibleFileNode(myProject, psiFile, settings, context)
      }
      else {
        CCStudentInvisibleFileNode(myProject, psiFile, settings, context, getTestNodeName(configurator, value))
      }
    }
    return null
  }

  override fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return CCNode(myProject, value, settings, context, item)
  }

  override fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): PsiFileNode {
    return CCFileNode(myProject, psiFile, settings, context)
  }

  companion object {
    private fun getTestNodeName(configurator: EduConfigurator<*>, psiElement: PsiElement): String {
      val defaultTestName = configurator.testFileName
      if (psiElement is PsiFile) {
        return defaultTestName
      }
      if (psiElement is PsiNamedElement) {
        val name = psiElement.name
        return name ?: defaultTestName
      }
      return defaultTestName
    }
  }
}
