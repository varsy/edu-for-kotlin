package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.authorContentsStorage.FileContentsHolderInProjectStorage
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
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, HIGHLIGHT_LEVEL, TEXT, LEARNER_CREATED)
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {

  @JsonProperty(LEARNER_CREATED)
  private var isLearnerCreated = false
}

class StudentTaskFileBuilder(
  @JsonProperty(TEXT) val textFromConfig: String?,
  @Encrypt @JsonProperty(ENCRYPTED_TEXT) val encryptedTextFromConfig: String?,
  @JsonProperty(LEARNER_CREATED) val learnerCreated: Boolean = false,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true,
  editable: Boolean = true,
  @JsonProperty(HIGHLIGHT_LEVEL) errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
) : TaskFileBuilder(name, placeholders, visible, editable, errorHighlightLevel) {
  override fun createTaskFile(): TaskFile {
    val taskFile = super.createTaskFile()

    // textFromConfig and encryptedTextFromConfig are legacy values that might apper in yamls created by older versions of the plugin
    if (encryptedTextFromConfig != null) {
      taskFile.contents = TextualContents(encryptedTextFromConfig)
    }
    else if (textFromConfig != null) {
      taskFile.contents = TextualContents(textFromConfig)
    }
    else {
      taskFile.contentsHolder = FileContentsHolderInProjectStorage(taskFile)
    }

    taskFile.isLearnerCreated = learnerCreated

    return taskFile
  }
}
