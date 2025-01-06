package com.jetbrains.edu.learning.configuration

interface ArchiveFileInfo {

  /**
   * Whether a file should be excluded from a course archive automatically.
   * This value marks files excluded from a course archive in previous versions of the plugin, when additional files were not explicitly
   * listed in the `course-info.yaml`.
   * To preserve the old behavior, the value is used when the course is migrated from a version without an implicit list of additional
   * files (YAML before v2).
   */
  val excludedFromArchive : Boolean

  /**
   * Special meaning of the file, displayed in hints or warnings for course-info.yaml
   */
  val description: String?

  /**
   * Specified a hint or a warning type for the course-info.yaml
   */
  val includeType: IncludeType

  /**
   * The file should be displayed in the course view
   */
  val showInCourseView: Boolean
}

enum class IncludeType {
  NO_MATTER,
  BETTER_INCLUDE,
  BETTER_NOT_INCLUDE,
  MUST_INCLUDE,
  MUST_NOT_INCLUDE
}