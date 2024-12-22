package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode

open class SectionNode(
  project: Project,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  section: Section,
  psiDirectory: PsiDirectory
) : EduNode<Section>(project, psiDirectory, viewSettings, context, section) {

  override val item: Section get() = super.item!!

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val lesson = item.getLesson(directory.name) ?: return null
    return createLessonNode(directory, lesson)
  }

  protected open fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? {
    return if (lesson is FrameworkLesson) {
      createFrameworkLessonNode(myProject, directory, settings, context, lesson)
    }
    else {
      LessonNode(myProject, directory, settings, context, lesson)
    }
  }

  override fun getWeight(): Int = item.index
}
