package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

open class CourseNode(
  project: Project,
  value: PsiDirectory,
  settings: ViewSettings,
  context: CourseViewContext
) : EduNode<Course>(project, value, settings, context, context.course) {

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val section = item.getSection(directory.name)
    if (section != null) {
      return createSectionNode(directory, section)
    }
    val lesson = item.getLesson(directory.name)
    if (lesson != null) {
      val lessonSolved = lesson.taskList.all { it.status == CheckStatus.Solved }
      if (lessonSolved && PropertiesComponent.getInstance().getBoolean(CourseViewPane.HIDE_SOLVED_LESSONS, false)) {
        return null
      }
      return createLessonNode(directory, lesson)
    }
    return null
  }

  protected open fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return SectionNode(myProject, settings, context, section, directory)
  }

  protected open fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? {
    return if (lesson is FrameworkLesson) {
      createFrameworkLessonNode(myProject, directory, settings, context, lesson)
    }
    else {
      LessonNode(myProject, directory, settings, context, lesson)
    }
  }

  override val additionalInfo: String?
    get() {
      if (item is HyperskillCourse) {
        return null
      }
      val (tasksSolved, tasksTotal) = ProgressUtil.countProgress(item)
      return " $tasksSolved/$tasksTotal"
    }

  override val item: Course get() = super.item!!
}
