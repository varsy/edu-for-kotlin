package com.jetbrains.edu.learning.courseFormat

open class EduFile {
  var name: String = ""

  /**
   * The contents of this edu file encoded as a string.
   * If the file is textual, the contents are stored as is.
   * If the contents are binary, it is stored as a base64 encoded string.
   *
   * In the student mode, this field most of the time stores the initial contents of the file, i.e. the contents provided by the course author.
   * In the course creation mode, in tests, and sometimes in the student mode, this field stores the contents that are about to be written
   * to disk.
   *
   * It is preferable to use the [contents] property instead of the [text] property, because for the [text] property it is impossible to tell
   * whether the contents are binary or textual.
   * Currently, the [text] field delegates to the [contents] field.
   */
  var text: String
    @Deprecated("Use EduFile.contents to be sure, whether this data is textual ")
    get() = contents.textualRepresentation
    @Deprecated("Use EduFile.contents to specify explicitly is it binary or not")
    set(value) {
      contents = InMemoryUndeterminedContents(value)
    }

  /**
   * See the [text] field for the description.
   * The [contents] field, compared to the text field, also contains the information about whether the contents are binary or not.
   */
  var contents: FileContents = UndeterminedContents.EMPTY
  @Suppress("unused") // used for serialization
  val isBinary: Boolean?
    get() {
      return contents.isBinary
    }

  var isTrackChanges: Boolean = true
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  var isVisible: Boolean = true
  var isEditable: Boolean = true

  // Should be used only in student mode
  var isLearnerCreated: Boolean = false

  constructor()
  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }

  constructor(name: String, text: String, isVisible: Boolean) : this(name, text) {
    this.isVisible = isVisible
  }

  constructor(name: String, text: String, isVisible: Boolean, isLearnerCreated: Boolean) : this(name, text, isVisible) {
    this.isLearnerCreated = isLearnerCreated
  }

  @Suppress("unused") // used for serialization
  fun getTextToSerialize(): String? {
    if (exceedsBase64ContentLimit(text)) {
      LOG.warning("Base64 encoding of `$name` file exceeds limit (${getBinaryFileLimit().toLong()}), " +
               "its content isn't serialized")
      return null
    }
    val contentType = mimeFileType(name) ?: return text
    return if (isBinary(contentType)) null else text
  }

  companion object {
    val LOG = logger<EduFile>()
  }
}