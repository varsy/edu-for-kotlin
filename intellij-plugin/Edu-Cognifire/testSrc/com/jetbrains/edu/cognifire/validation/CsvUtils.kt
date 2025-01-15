package com.jetbrains.edu.cognifire.validation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class PromptWithTodoDataframeRecord(
  @JsonProperty("prompt")
  val prompt: String,
  @JsonProperty("sentencesIndicesWithTodo")
  @JsonDeserialize(using = StringToListOfIntDeserializer::class)
  val sentencesIndicesWithTodo: List<Int>
)

data class ValidationResultsDataframeRecord(
  val timestamp: String,
  val todoCompleteness: String
)