package com.jetbrains.edu.aiHints.core.context

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class FunctionParameter
@JsonCreator constructor(
  @JsonProperty(PARAMETER_NAME) val name: String,
  @JsonProperty(TYPE) val type: String
) {
  override fun toString() = "$name$NAME_TYPE_SEPARATOR$type"

  companion object {
    private const val PARAMETER_NAME = "name"
    private const val TYPE = "type"

    private const val NAME_TYPE_SEPARATOR = ": "
  }
}