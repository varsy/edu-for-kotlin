package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.TaskFileBuilder
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HIGHLIGHT_LEVEL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, HIGHLIGHT_LEVEL, TEXT, "is_binary", LEARNER_CREATED)
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {

  private val isBinary: Boolean?
    @JsonProperty("is_binary")
    @JsonInclude(value=JsonInclude.Include.CUSTOM, valueFilter = FalseFileter::class)
    get() = null


  @JsonProperty(TEXT)
  open fun getTextToSerialize(): String {
    throw NotImplementedError()
  }

  @JsonProperty(LEARNER_CREATED)
  private var isLearnerCreated = false
}

@Suppress("EqualsOrHashCode")
class FalseFileter {
  override fun equals(other: Any?) = other == false
}

class StudentTaskFileBuilder(
  @JsonProperty(TEXT) val textFromConfig: String?,
  @Encrypt @JsonProperty(ENCRYPTED_TEXT) val encryptedTextFromConfig: String?,
  @JsonProperty(LEARNER_CREATED) val learnerCreated: Boolean = false,
  @JsonProperty("is_binary") val isBinary1: Boolean? = false,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true,
  editable: Boolean = true,
  @JsonProperty(HIGHLIGHT_LEVEL) errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
) : TaskFileBuilder(name, placeholders, visible, editable, errorHighlightLevel) {
  override fun createTaskFile(): TaskFile {
    return super.createTaskFile().apply {
      val text = encryptedTextFromConfig ?: textFromConfig ?: ""

      contents = if (isBinary1 == true) {
        InMemoryBinaryContents.parseBase64Encoding(text)
      }
      else {
        InMemoryUndeterminedContents(text)
      }
      isLearnerCreated = learnerCreated
    }
  }
}