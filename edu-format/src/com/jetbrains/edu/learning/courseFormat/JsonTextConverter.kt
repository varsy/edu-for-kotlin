package com.jetbrains.edu.learning.courseFormat

/**
 * For [EduFile]s inside JSON, converts their `text` field to [FileContents].
 *
 * The contents of files stored inside the `text` fields might be very long somtimes, so it
 * occupies a huge amount of memory.
 * For that reason, we don't always want to load them into memory, and we need to specify how exactly
 * those contents are stored after reading JSON.
 *
 * For example, [JsonTextConverter] implementation might put the texts in some disk storage, so that
 * they are available, but don't ocupy memory.
 * The lifetime of that storage is a feature of a specific [JsonTextConverter] implementation.
 *
 * Despite that after JSON is read, the contents of files are stored in the place provided
 * by the JsonTextConverter implementation,
 * during reading JSON, all the contents still might occupy the memory.
 * Even the [OutOfMemoryError] might occur.
 * That is because the JSON reading is currently implemented not effectively.
 */
interface JsonTextConverter {

  fun getContents(textualRepresentation: String, isBinary: Boolean?): FileContents
}

/**
 * Stores contents of all [EduFile]s to memory.
 *
 * This conversion might occupy a huge amount of memory.
 * One should avoid using it unless it is known that file contents are small.
 */
object ToMemoryTextConverter : JsonTextConverter {
  override fun getContents(textualRepresentation: String, isBinary: Boolean?): FileContents = when (isBinary) {
    true -> InMemoryBinaryContents.parseBase64Encoding(textualRepresentation)
    false -> InMemoryTextualContents(textualRepresentation)
    null -> InMemoryUndeterminedContents(textualRepresentation)
  }
}

/**
 * Drops contents of all [EduFile]s and does not store them.
 *
 * This might be used when only a course's meta-information, such as its structure, is needed.
 */
object ToEmptyTextConverter: JsonTextConverter {
  override fun getContents(textualRepresentation: String, isBinary: Boolean?): FileContents = when (isBinary) {
    true -> BinaryContents.EMPTY
    false -> TextualContents.EMPTY
    null -> UndeterminedContents.EMPTY
  }
}