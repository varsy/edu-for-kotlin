package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ignore.cache.PatternCache
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreEntry
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreVisitor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseDir
import java.util.regex.Pattern

private data class IgnorePattern(val pattern: Pattern, val isNegated: Boolean)

class CourseIgnoreRulesFromFile(project: Project, courseIgnorePsiFile: PsiFile) : CourseIgnoreRules {

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

  override fun testIgnored(file: VirtualFile): Boolean? {
    val courseRelativePath = VfsUtil.getRelativePath(file, courseDir) ?: return false

    val courseRelativePathFixedForDirectory = if (file.isDirectory) {
      "$courseRelativePath/"
    }
    else {
      courseRelativePath
    }

    val matchingPattern = patterns.findLast { it.pattern.matcher(courseRelativePathFixedForDirectory).find() } ?: return null
    return !matchingPattern.isNegated
  }
}