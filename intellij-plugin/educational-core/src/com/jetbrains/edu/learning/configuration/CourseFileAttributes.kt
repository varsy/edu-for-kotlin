package com.jetbrains.edu.learning.configuration

data class CourseFileAttributes(
  /**
   * Whether a file should be excluded from a course archive automatically.
   * This value preserves the old behavior of the plugin, when additional files were not explicitly listed in the `course-info.yaml`.
   * That time all files from the disk were put to archive, except the files, for which `EduConfigurator.excludeFromArchive()` was `true`.
   * The value is used when the course is migrated from a version without an implicit list of additional files (YAML before v2).
   * Other usages of this field are considered legacy and should be eliminated with time.
   */
  val excludedFromArchive : Boolean

  /**
   * We are going to add here more information about the file:
   *  - show in course view or not (to hide out/build/target directories)
   *  - suggest including in archive: must be included/must not be included/better to be included/etc.
   */
)

class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(excludedFromArchive)

  var excludedFromArchive: Boolean = false
}