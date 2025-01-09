package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.learning.canBeAddedToTask
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.projectView.CourseViewContext
import com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation
import org.jetbrains.annotations.TestOnly

/**
 * Add to the file name postfix "course.creator.course.view.excluded" from EduCoreBundle.properties
 * if the file in .courseignore
 */
class CCStudentInvisibleFileNode(
  project: Project,
  value: PsiFile,
  viewSettings: ViewSettings,
  context: CourseViewContext,
  private val name: String = value.name
) : CCFileNode(project, value, viewSettings, context) {

  private fun needsExcludedMark(file: VirtualFile?, project: Project): Boolean {
    file ?: return false
    val task = file.getContainingTask(project)

    return if (task != null) {
      file.canBeAddedToTask(project)
    }
    else {
      !context.containsAdditionalFile(file) && !context.generatedPersonallyForStudent(file)
    }
  }

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)

    val file = value.virtualFile
    val isExcluded = needsExcludedMark(file, project)
    val presentableName = if (isExcluded) message("course.creator.course.view.excluded", name) else name

    data.clearText()
    data.addText(presentableName, SimpleTextAttributes.GRAY_ATTRIBUTES)
  }

  @Deprecated("Deprecated in Java",
    ReplaceWith("testPresentation(this)", "com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation")
  )
  @TestOnly
  override fun getTestPresentation(): String {
    return testPresentation(this)
  }

}
