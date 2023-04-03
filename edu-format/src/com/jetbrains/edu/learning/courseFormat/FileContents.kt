package com.jetbrains.edu.learning.courseFormat

import java.util.*

/**
 * [FileContents] is a representation of a contents of some file, together with the information, whether these contents
 * should be interpreted as binary or as textual.
 * There are two reasons to distinguish between two types of contents:
 *  1. The `course.json` format can store only textual information, so binary files should be encoded before they go into it.
 *  2. Text files preserve their contents independently on the encodings of a teacher's or a learner's computer.
 *
 *  This class may store contents in different places, for example, the contents may be stored in memory, go directly from disk,
 *  or may be stored inside a zip file.
 */
sealed class FileContents {
  /**
   * Whether the contents are binary or not.
   *
   * The value may be null if we don't know the type of contents.
   * In this case, some external ways to determine the types of contents should be used.
   * For example, the file name or its extension may be a hint to decide.
   */
  abstract val isBinary: Boolean?

  /**
   * This is a base64 encoding of a binary contents, a plain text for textual contents, or one of these variants,
   * if [isBinary] is `null`.
   */
  abstract val textualRepresentation: String
}

abstract class BinaryContents : FileContents() {

  abstract val bytes: ByteArray

  override val textualRepresentation: String
    get() = asBase64

  override val isBinary: Boolean?
    get() = true

  private val asBase64: String
    get() = Base64.getEncoder().encodeToString(bytes)

  companion object {
    fun unpackBase64(base64: String): FileContents =
      try {
        InMemoryBinaryContents(Base64.getDecoder().decode(base64))
      }
      catch (e: IllegalArgumentException) {
        UndeterminedContents.EMPTY
      }

    val EMPTY = object : BinaryContents() {
      override val bytes: ByteArray = byteArrayOf()
    }
  }
}

abstract class TextualContents : FileContents() {

  abstract val text: String

  override val isBinary: Boolean?
    get() = false

  override val textualRepresentation: String
    get() = text

  companion object {
    val EMPTY = object : TextualContents() {
      override val text = ""
    }
  }
}

abstract class UndeterminedContents : FileContents() {

  companion object {
    val EMPTY = object : UndeterminedContents() {
      override val textualRepresentation: String
        get() = ""
    }
  }

  override val isBinary: Boolean?
    get() = null

  fun interpretAsBinary(): BinaryContents {
    val self = this
    return object : BinaryContents() {
      override val bytes: ByteArray
        get() = Base64.getDecoder().decode(self.textualRepresentation)
    }
  }

  fun interpretAsTextual(): TextualContents {
    val self = this
    return object : TextualContents() {
      override val text: String
        get() = self.textualRepresentation
    }
  }
}

class InMemoryBinaryContents(override val bytes: ByteArray): BinaryContents()
class InMemoryTextualContents(override val text: String) : TextualContents()
class InMemoryUndeterminedContents(override val textualRepresentation: String) : UndeterminedContents()

/**
 * Creates a [FileContents] that stores its information in memory.
 */
fun inMemoryFileContentsFromText(text: String, isBinary: Boolean?): FileContents {
  return when (isBinary) {
    true -> BinaryContents.unpackBase64(text)
    false -> InMemoryTextualContents(text)
    null -> InMemoryUndeterminedContents(text)
  }
}