package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.*
import java.nio.charset.StandardCharsets

/**
 * Creates a [FileContents] instance, that takes its contents from some [AuthorContentsStorage].
 * Arguments [pathProvider] and [authorContentsStorageProvider] are lazy because the path and the storage
 * could be unknown at the moment of the call.
 */
fun fileContentsFromAuthorContentsStorage(
  pathProvider: () -> String?,
  isBinary: Boolean?,
  authorContentsStorageProvider: () -> AuthorContentsStorage?
): FileContents = when (isBinary) {
  true -> BinaryContentsFromAuthorContentsStorage(pathProvider, authorContentsStorageProvider)
  false -> TextualContentsFromAuthorContentsStorage(pathProvider, authorContentsStorageProvider)
  null -> UndeterminedContentsFromAuthorContentsStorage(pathProvider, authorContentsStorageProvider)
}

/**
 * Gets the path to a file inside a course folder
 */
fun pathInAuthorContentsStorageForEduFile(eduFile: EduFile): String {
  if (eduFile is TaskFile) {
    return "${eduFile.task.getPathInCourse()}/${eduFile.name}"
  }

  return eduFile.name
}

private val EMPTY_BYTE_ARRAY = byteArrayOf()

private fun getBytes(pathProvider: () -> String?, authorContentsStorageProvider: () -> AuthorContentsStorage?): ByteArray {
  val authorContentsStorage = authorContentsStorageProvider() ?: return EMPTY_BYTE_ARRAY
  val path = pathProvider() ?: return EMPTY_BYTE_ARRAY

  return authorContentsStorage.get(path) ?: EMPTY_BYTE_ARRAY
}

private class TextualContentsFromAuthorContentsStorage(
  private val pathProvider: () -> String?,
  private val authorContentsStorageProvider: () -> AuthorContentsStorage?
) : TextualContents() {
  override val text: String
    get() = String(getBytes(pathProvider, authorContentsStorageProvider), StandardCharsets.UTF_8)
}

private class BinaryContentsFromAuthorContentsStorage(
  private val pathProvider: () -> String?,
  private val authorContentsStorageProvider: () -> AuthorContentsStorage?
) : BinaryContents() {
  override val bytes: ByteArray
    get() = getBytes(pathProvider, authorContentsStorageProvider)
}

private class UndeterminedContentsFromAuthorContentsStorage(
  private val pathProvider: () -> String?,
  private val authorContentsStorageProvider: () -> AuthorContentsStorage?
) : UndeterminedContents() {
  override val textualRepresentation: String
    get() = String(getBytes(pathProvider, authorContentsStorageProvider), StandardCharsets.UTF_8)
}