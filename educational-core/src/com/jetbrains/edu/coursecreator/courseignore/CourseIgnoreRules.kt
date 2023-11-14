package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import java.util.regex.Pattern

interface CourseIgnoreRules {
  fun isIgnored(file: VirtualFile): Boolean = testIgnored(file) ?: false
  fun testIgnored(file: VirtualFile): Boolean?

  companion object {

    fun loadFromCourseIgnoreFile(project: Project): CourseIgnoreRules = runReadAction {
      val courseIgnoreVirtualFile = project.courseDir.findChild(EduNames.COURSE_IGNORE)
                                    ?: return@runReadAction EMPTY
      val courseIgnorePsiFile = PsiManager.getInstance(project).findFile(courseIgnoreVirtualFile)
                                ?: return@runReadAction EMPTY

      CachedValuesManager.getCachedValue(courseIgnorePsiFile) {
        CachedValueProvider.Result(
          CourseIgnoreRulesFromFile(project, courseIgnorePsiFile),
          courseIgnorePsiFile
        )
      }
    }

    private val EMPTY: CourseIgnoreRules = object : CourseIgnoreRules {
      override fun isIgnored(file: VirtualFile): Boolean = false
      override fun testIgnored(file: VirtualFile): Boolean = false
    }
  }
}