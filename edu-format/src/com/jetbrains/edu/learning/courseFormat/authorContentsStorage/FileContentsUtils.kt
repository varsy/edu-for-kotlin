package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TaskFile

/**
 * Gets the path to a [EduFile] inside a course folder.
 * This path is used as a key to a [FileContents] inside an [AuthorContentsStorage].
 */
fun pathInAuthorContentsStorageForEduFile(eduFile: EduFile): String {
  if (eduFile is TaskFile) {
    return "${eduFile.task.getPathInCourse()}/${eduFile.name}"
  }

  return eduFile.name
}