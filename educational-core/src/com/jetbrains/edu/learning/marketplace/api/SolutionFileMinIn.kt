package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames

@JsonPropertyOrder(
  JsonMixinNames.NAME,
  JsonMixinNames.PLACEHOLDERS,
  JsonMixinNames.IS_VISIBLE,
  JsonMixinNames.TEXT
)
abstract class SolutionFileMinIn {
  @JsonProperty(JsonMixinNames.NAME)
  private lateinit var name: String

  @JsonProperty(JsonMixinNames.PLACEHOLDERS)
  private lateinit var _answerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(JsonMixinNames.IS_VISIBLE)
  private var isVisible: Boolean = true

  @JsonProperty(JsonMixinNames.TEXT)
  @Encrypt
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var text: String
}