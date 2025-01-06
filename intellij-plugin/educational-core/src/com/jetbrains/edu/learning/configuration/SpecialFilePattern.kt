package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course

fun buildArchiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile, actions: ArchiveInfoBuilder.() -> Unit): ArchiveFileInfo =
  ArchiveInfoBuilder(holder, file).apply(actions).generate()

class ArchiveInfoBuilder(private val holder: CourseInfoHolder<out Course?>, private val file: VirtualFile) {

  private var excludedFromArchive: Boolean = true
  private var description: String? = null
  private var includeType: IncludeType = IncludeType.NO_MATTER
  private var showInCourseView: Boolean = true

  fun includeInArchive() {
    excludedFromArchive = false
  }

  fun description(@NlsContexts.DetailedDescription description: String) {
    this.description = description
  }

  fun type(includeType: IncludeType) {
    this.includeType = includeType
  }

  fun hideInCourseView() {
    this.showInCourseView = false
  }

  fun info(value: ArchiveFileInfo) {
    this.excludedFromArchive = value.excludedFromArchive
    this.description = value.description
    this.includeType = value.includeType
    this.showInCourseView = value.showInCourseView
  }

  private class Info(
    override val excludedFromArchive: Boolean,
    override val description: String?,
    override val includeType: IncludeType,
    override val showInCourseView: Boolean,
  ) : ArchiveFileInfo

  fun generate(): ArchiveFileInfo = Info(excludedFromArchive, description, includeType, showInCourseView)

  fun regex(pattern: String): Boolean = pathFilter {
    Regex(pattern).containsMatchIn(it)
  }

  fun nameRegex(pattern: String) = Regex(pattern).containsMatchIn(file.name)

  fun pathFilter(filter: (String) -> Boolean): Boolean {
    val path = FileUtil.getRelativePath(holder.courseDir.path, file.path, VFS_SEPARATOR_CHAR) ?: return false

    val fixedPath = if (path == ".") {
      "/"
    }
    else {
      path + if (file.isDirectory) "/" else ""
    }

    return filter(fixedPath)
  }
}