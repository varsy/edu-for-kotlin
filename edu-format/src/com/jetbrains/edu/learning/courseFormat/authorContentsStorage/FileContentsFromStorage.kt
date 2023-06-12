package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.fileContents.FileContents
import com.jetbrains.edu.learning.courseFormat.fileContents.FileContentsHolder
import com.jetbrains.edu.learning.courseFormat.fileContents.UndeterminedContents

/**
 * A [FileContentsHolder] that stores [FileContents] inside some [AuthorContentsStorage].
 * The path inside the storage corresponds to the [eduFile] parameter.
 */
abstract class StorageAuthorContentsHolder(val eduFile: EduFile) : FileContentsHolder {
  abstract val storage: AuthorContentsStorage?

  val path: String
    get() {
      if (eduFile is TaskFile) {
        return "${eduFile.task.getPathInCourse()}/${eduFile.name}"
      }

      return eduFile.name
    }

  override fun get(): FileContents = storage?.get(path) ?: UndeterminedContents.EMPTY

  override fun set(value: FileContents) {
    storage?.put(path, value)
  }
}