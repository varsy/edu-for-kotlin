package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.fileContentsFromAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.TaskFileBuilder
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_BINARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, TEXT, IS_BINARY, LEARNER_CREATED)
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {

  @JsonProperty(LEARNER_CREATED)
  private var isLearnerCreated = false
}

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, TEXT, IS_BINARY, LEARNER_CREATED)
abstract class StudentTaskFileYamlMixinWithText : StudentTaskFileYamlMixin() {

  @JsonProperty(TEXT)
  open fun getTextToSerialize(): String {
    throw NotImplementedError()
  }
}

class StudentTaskFileBuilder(
  @JsonProperty(TEXT) val textFromConfig: String?,
  @Encrypt @JsonProperty(ENCRYPTED_TEXT) val encryptedTextFromConfig: String?,
  @JsonProperty(IS_BINARY) val isBinary: Boolean?,
  @JsonProperty(LEARNER_CREATED) val learnerCreated: Boolean = false,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true,
  editable: Boolean = true
) : TaskFileBuilder(name, placeholders, visible, editable) {
  override fun createTaskFile(): TaskFile {
    val taskFile = super.createTaskFile()

    // textFromConfig and encryptedTextFromConfig are legacy values that might apper in yamls created by older versions of the plugin
    taskFile.contents = if (encryptedTextFromConfig != null) {
      InMemoryTextualContents(encryptedTextFromConfig)
    }
    else if (textFromConfig != null) {
      InMemoryTextualContents(textFromConfig)
    }
    else {
      fileContentsFromAuthorContentsStorage({ pathInAuthorContentsStorageForEduFile(taskFile) }, isBinary) {
        val project = taskFile.course?.project
        val manager = project?.let { StudyTaskManager.getInstance(it) }
        manager?.authorContentsStorage
      }
    }

    taskFile.isLearnerCreated = learnerCreated

    return taskFile
  }
}
