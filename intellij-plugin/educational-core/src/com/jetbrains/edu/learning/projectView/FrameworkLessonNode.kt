package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

class FrameworkLessonNode private constructor(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  lesson: FrameworkLesson
) : LessonNode(project, value, viewSettings, context, lesson) {

  override val item: FrameworkLesson
    get() = super.item as FrameworkLesson

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val task = item.currentTask() ?: return null
    return CourseViewUtils.modifyTaskChildNode(myProject, childNode, task, ::createChildFileNode) {
      dir -> DirectoryNode(myProject, dir, settings, context, task)
    }
  }

  override fun canNavigate(): Boolean = item.taskList.isNotEmpty()

  override fun expandOnDoubleClick(): Boolean = false

  override fun navigate(requestFocus: Boolean) {
    val firstTask = item.currentTask() ?: return
    NavigationUtils.navigateToTask(myProject, firstTask)
  }

  override val additionalInfo: String?
    get() {
      val course = item.course
      return if (course is HyperskillCourse && course.isStudy && item == course.getProjectLesson()) {
        val (tasksSolved, tasksTotal) = ProgressUtil.countProgress(item)
        if (tasksTotal == 0) {
          return null
        }
        return EduCoreBundle.message("hyperskill.course.view.progress", tasksSolved, tasksTotal)
      }
      else super.additionalInfo
    }

  private fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): AbstractTreeNode<*> {
    return originalNode
  }

  companion object {

    fun createFrameworkLessonNode(
      project: Project,
      lessonDirectory: PsiDirectory,
      viewSettings: ViewSettings,
      context: CourseViewContext,
      lesson: FrameworkLesson
    ): FrameworkLessonNode? {
      val task = lesson.currentTask()
      val dir = if (task != null) {
        val taskBaseDirectory = lessonDirectory.findSubdirectory(TASK) ?: return null
        CourseViewUtils.findTaskDirectory(project, taskBaseDirectory, task) ?: return null
      }
      else {
        lessonDirectory
      }
      return FrameworkLessonNode(project, dir, viewSettings, context, lesson)
    }
  }
}
