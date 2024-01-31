package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ignore.cache.PatternCache
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreEntry
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreVisitor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import java.util.regex.Pattern

private data class IgnorePattern(val pattern: Pattern, val isNegated: Boolean)

interface CourseIgnoreRules {
  fun isIgnored(file: VirtualFile): Boolean

  companion object {

    fun loadFromCourseIgnoreFile(project: Project): CourseIgnoreRules = runReadAction {
      val courseIgnoreVirtualFile = project.courseDir.findChild(EduNames.COURSE_IGNORE)
                                    ?: return@runReadAction EMPTY
      val courseIgnorePsiFile = PsiManager.getInstance(project).findFile(courseIgnoreVirtualFile)
                                ?: return@runReadAction EMPTY

      CachedValuesManager.getCachedValue(courseIgnorePsiFile) {
        val psiFileToParse = if (courseIgnorePsiFile.fileType == CourseIgnoreFileType) {
          courseIgnorePsiFile
        }
        else {
          val text = courseIgnorePsiFile.text
          PsiFileFactory.getInstance(project).createFileFromText(CourseIgnoreLanguage, text)
        }

        val rules = CourseIgnoreRulesFromFile(project, psiFileToParse)

        CachedValueProvider.Result(rules, courseIgnorePsiFile)
      }
    }

    fun fromIgnoringEntries(project: Project, entries: List<IgnoringEntry>): CourseIgnoreRules {
      val ignoreText = entries.flatMap { it.courseignoreLines }.joinToString("\n")
      val psiFileToParse = PsiFileFactory.getInstance(project).createFileFromText(CourseIgnoreLanguage, ignoreText)
      return CourseIgnoreRulesFromFile(project, psiFileToParse)
    }

    val EMPTY: CourseIgnoreRules = object : CourseIgnoreRules {
      override fun isIgnored(file: VirtualFile): Boolean = false
    }
  }
}

private class CourseIgnoreRulesFromFile(project: Project, courseIgnorePsiFile: PsiFile) : CourseIgnoreRules {

  private val courseDir: VirtualFile
  private val patterns: List<IgnorePattern>

  init {
    val patternCache = PatternCache.getInstance(project)

    patterns = mutableListOf()

    courseIgnorePsiFile.acceptChildren(object : IgnoreVisitor() {
      override fun visitEntry(ignoreEntry: IgnoreEntry) {
        super.visitEntry(ignoreEntry)
        val pattern = patternCache.createPattern(ignoreEntry) ?: return
        patterns.add(IgnorePattern(pattern, ignoreEntry.isNegated))
      }
    })

    courseDir = project.courseDir
  }

  override fun isIgnored(file: VirtualFile): Boolean {
    val courseRelativePath = VfsUtil.getRelativePath(file, courseDir) ?: return false

    val courseRelativePathFixedForDirectory = if (file.isDirectory) {
      "$courseRelativePath/"
    }
    else {
      courseRelativePath
    }

    val matchingPattern = patterns.findLast { it.pattern.matcher(courseRelativePathFixedForDirectory).find() } ?: return false
    return !matchingPattern.isNegated
  }
}