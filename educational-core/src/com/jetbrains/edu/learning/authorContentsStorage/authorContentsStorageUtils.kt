package com.jetbrains.edu.learning.authorContentsStorage

import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.LazyFileContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.FileContents

/**
 * This method creates a [FileContents] for [EduFile] in such a way, that the contents are taken
 * from the project author contents storage.
 */
fun fileContentsFromProjectAuthorContentsStorage(eduFile: EduFile) = LazyFileContents {
  val project = eduFile.course?.project
  val manager = project?.let { StudyTaskManager.getInstance(it) }
  val storage = manager?.authorContentsStorage
  val path = pathInAuthorContentsStorageForEduFile(eduFile)
  storage?.get(path)
}