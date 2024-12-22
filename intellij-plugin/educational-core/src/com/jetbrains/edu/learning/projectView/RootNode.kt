package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.projectView.CCCourseNode
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode

class RootNode(project: Project, viewSettings: ViewSettings?) : ProjectViewProjectNode(project, viewSettings) {

  override fun getChildren(): Collection<AbstractTreeNode<*>> {
    val course = StudyTaskManager.getInstance(myProject).course ?: return emptyList()
      val nodes = ArrayList<AbstractTreeNode<*>>()
      if (!isUnitTestMode) {
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(myProject.courseDir)
        addCourseNode(course, nodes, psiDirectory)
      }
      else {
        val topLevelContentRoots = ProjectViewDirectoryHelper.getInstance(myProject).topLevelRoots
        for (root in topLevelContentRoots) {
          val psiDirectory = PsiManager.getInstance(myProject).findDirectory(root)
          addCourseNode(course, nodes, psiDirectory)
        }
      }
      return nodes
  }

  private fun addCourseNode(course: Course, nodes: MutableList<AbstractTreeNode<*>>, psiDirectory: PsiDirectory?) {
    if (psiDirectory == null) return
    val context = CourseViewContext.create(myProject, course) ?: return
    nodes += if (isCourseCreator(myProject)) {
      CCCourseNode(myProject, psiDirectory, settings, context)
    }
    else {
      CourseNode(myProject, psiDirectory, settings, context)
    }
  }
}
