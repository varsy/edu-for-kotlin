package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_BINARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, ENCRYPTED_TEXT, IS_BINARY, LEARNER_CREATED)
abstract class StudentEncryptedTaskFileYamlMixin : StudentTaskFileYamlMixin() {

  @JsonProperty(ENCRYPTED_TEXT)
  @Encrypt
  fun getTextToSerialize(): String {
    throw NotImplementedError()
  }
}

