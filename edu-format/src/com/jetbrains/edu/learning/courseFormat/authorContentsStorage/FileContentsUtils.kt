package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.logger

private val EMPTY_BYTE_ARRAY = byteArrayOf()

/**
 * This is a FileContents, that delegates everything to another FileContents that may not be known at the moment
 * of LazyFileContents instantiation.
 * It is needed for EduFile deserialization because at the moment of deserialization, the author contents storage is
 * only being built.
 */
class LazyFileContents(val fileContentsProvider: () -> FileContents?) : TextualContents, BinaryContents {
  private val delegate: FileContents? by lazy {
    fileContentsProvider()
  }

  override val text: String
    get() = warnIfNull((delegate as? TextualContents)?.text) ?: ""

  override val bytes: ByteArray
    get() = warnIfNull((delegate as? BinaryContents)?.bytes) ?: EMPTY_BYTE_ARRAY

  override val isBinary: Boolean?
    get() = warnIfNull(delegate?.isBinary)

  override val textualRepresentation: String
    get() = warnIfNull(delegate?.textualRepresentation) ?: ""

  private fun <T> warnIfNull(value: T?): T? {
    if (value == null)
      LOG.warning("Delegated file contents could not be retrieved")
    return value
  }

  companion object {
    val LOG = logger<LazyFileContents>()
  }
}

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