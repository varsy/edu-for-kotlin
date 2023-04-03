package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames
import com.jetbrains.edu.learning.json.mixins.TrueValueFilter
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  var isBinary: Boolean? = null
    @JsonProperty(JsonMixinNames.IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    get

  @JsonProperty(PLACEHOLDERS)
  private lateinit var _answerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(VISIBLE)
  private var isVisible = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(EDITABLE)
  private var isEditable = true
}

@JsonPOJOBuilder(withPrefix = "")
open class TaskFileBuilder(
  @JsonProperty(NAME) val name: String?,
  @JsonSetter(contentNulls = Nulls.SKIP)
  @JsonProperty(PLACEHOLDERS) val placeholders: List<AnswerPlaceholder> = mutableListOf(),
  @JsonProperty(VISIBLE) val visible: Boolean = true,
  @JsonProperty(EDITABLE) val editable: Boolean = true
) {
  @Suppress("unused") //used for deserialization
  private fun build(): TaskFile {
    if (name == null) {
      formatError(EduCoreBundle.message("yaml.editor.invalid.file.without.name"))
    }
    return createTaskFile()
  }

  protected open fun createTaskFile(): TaskFile {
    val taskFile = TaskFile()
    taskFile.name = name ?: ""
    taskFile.answerPlaceholders = placeholders
    taskFile.isVisible = visible
    taskFile.isEditable = editable

    return taskFile
  }
}