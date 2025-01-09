package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDirectory
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseNode
import com.jetbrains.edu.learning.projectView.LessonNode
import com.jetbrains.edu.learning.projectView.SectionNode
import com.jetbrains.edu.learning.toCourseInfoHolder
import org.jetbrains.annotations.Nls

class CCCourseNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  course: Course
) : CourseNode(project, value, viewSettings, course) {

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) return node
    if (childNode is PsiFileNode) {
      val virtualFile = childNode.virtualFile ?: return null
      if (NAMES_TO_IGNORE.contains(virtualFile.name)) return null
      if (FileUtilRt.getExtension(virtualFile.name) == "iml") return null
      return CCStudentInvisibleFileNode(myProject, childNode.value, settings)
    }
    val configurator: EduConfigurator<*> = item.configurator ?: return null
    if (childNode is PsiDirectoryNode) {
      val psiDirectory = childNode.value
      val info = configurator.archiveFileInfo(myProject.toCourseInfoHolder(), psiDirectory.virtualFile)
      if (info.includeType != IncludeType.MUST_NOT_INCLUDE_AND_HIDE && info.includeType != IncludeType.MUST_NOT_INCLUDE) {
        return CCNode(myProject, psiDirectory, settings, null)
      }
    }
    return null
  }

  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(myProject, directory, settings, lesson)
  }

  override fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return CCSectionNode(myProject, settings, section, directory)
  }

  override val additionalInfo: String
    @Nls get() = "(${EduCoreBundle.message("course.creator.course.view.course.creation")})"

  companion object {
    private val NAMES_TO_IGNORE: Collection<String> = ContainerUtil.newHashSet(LOCAL_PROPERTIES, GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN)
  }
}