package com.jetbrains.edu.learning.courseFormat

import java.util.*

/**
 * [FileContents] is a representation of some file contents, together with the information whether these contents
 * should be interpreted as binary or as textual.
 * There are two reasons to distinguish between two types of contents:
 *  1. The `course.json` format can store only textual information, so binary files should be encoded before they go into it.
 *  2. Text files preserve their contents independently on the encodings of a teacher's or a learner's computer.
 *
 *  This class may represent contents stored in different places, for example, the contents may be stored in memory, come directly from disk,
 *  or may be stored inside a zip file.
 */
interface FileContents {
  /**
   * Whether the contents are binary or not.
   *
   * The value may be null if we don't know the type of contents.
   * In this case, some external ways to determine the types of contents should be used.
   * For example, the file name or its extension may be a hint to decide.
   *
   * If the value of [isBinary] is `true`, the class instance must also implement the [BinaryContents] interface.
   * If the value of [isBinary] is `false`, the class instance must also implement the [TextualContents] interface.
   * If the value is `null`, both interfaces must be implemented.
   */
  val isBinary: Boolean?

  /**
   * This is a base64 encoding of a binary contents, a plain text for textual contents, or one of these variants,
   * if [isBinary] is `null`.
   */
  val textualRepresentation: String
}

interface TextualContents : FileContents {

  val text: String

  override val isBinary: Boolean?
    get() = false

  override val textualRepresentation: String
    get() = text

  companion object {
    val EMPTY = object : TextualContents {
      override val text
        get() = ""
    }
  }
}

interface BinaryContents : FileContents {

  val bytes: ByteArray

  override val textualRepresentation: String
    get() = Base64.getEncoder().encodeToString(bytes)

  override val isBinary: Boolean?
    get() = true

  companion object {
    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    val EMPTY = object : BinaryContents {
      override val bytes: ByteArray
        get() = EMPTY_BYTE_ARRAY
    }
  }
}

/**
 * This interface represents file contents, for which we know only its textual representation and don't know whether it is
 * a text or a base64 encoding of a byte array.
 * When we determine this somehow externally, for example, by the file name,
 * we retrieve a value of either a [text] field, or a [bytes] field.
 * If we determine the actual type incorrectly, we get broken data, or even get an exception, if it is impossible
 * to parse base64 encoding of a [textualRepresentation].
 */
interface UndeterminedContents : TextualContents, BinaryContents {

  override val textualRepresentation: String

  override val isBinary: Boolean?
    get() = null

  override val text: String
    get() = textualRepresentation

  override val bytes: ByteArray
    get() = Base64.getDecoder().decode(textualRepresentation)

  companion object {
    val EMPTY = object : UndeterminedContents {
      override val textualRepresentation: String = ""
    }
  }
}

/**
 * Represents a binary FileContents stored in memory.
 * These contents are not persistent.
 */
class InMemoryBinaryContents(override val bytes: ByteArray): BinaryContents

/**
 * Represents a textual FileContents stored in memory.
 * These contents are not persistent.
 */
class InMemoryTextualContents(override val text: String) : TextualContents

/**
 * Represents a FileContents of an unknown type stored in memory.
 * These contents are not persistent.
 */
class InMemoryUndeterminedContents(override val textualRepresentation: String) : UndeterminedContents

/**
 * Creates a [FileContents] that stores its information in memory.
 */
fun inMemoryFileContentsFromText(text: String, isBinary: Boolean?): FileContents {
  return when (isBinary) {
    true -> InMemoryBinaryContents(Base64.getDecoder().decode(text))
    false -> InMemoryTextualContents(text)
    null -> InMemoryUndeterminedContents(text)
  }
}