package com.jetbrains.edu.learning.courseFormat.eduAssistant

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionParameterJsonNames.PARAMETER_NAME
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionParameterJsonNames.TYPE
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignatureJsonNames.BODY_LINE_COUNT
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignatureJsonNames.PARAMETERS
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignatureJsonNames.RETURN_TYPE
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignatureJsonNames.SIGNATURE_NAME
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignatureJsonNames.SIGNATURE_SOURCE

data class FunctionSignature
@JsonCreator constructor(
  @JsonProperty(SIGNATURE_NAME) val name: String,
  @JsonProperty(PARAMETERS) val parameters: List<FunctionParameter>,
  @JsonProperty(RETURN_TYPE) val returnType: String,
  @JsonProperty(SIGNATURE_SOURCE) val signatureSource: SignatureSource? = null,
  @JsonProperty(BODY_LINE_COUNT) val bodyLineCount: Int? = null
) {
  override fun toString(): String {
    return "$name$NAME_ARGS_SEPARATOR${parameters.joinToString(separator = ARGS_SEPARATOR)}$ARGS_RETURN_TYPE_SEPARATOR$returnType"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FunctionSignature

    if (name != other.name) return false
    if (parameters != other.parameters) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + parameters.hashCode()
    return result
  }

  companion object {
    private const val NAME_ARGS_SEPARATOR = "("
    private const val ARGS_RETURN_TYPE_SEPARATOR = "): "
    private const val ARGS_SEPARATOR = ", "
  }
}

data class FunctionParameter
@JsonCreator constructor(
  @JsonProperty(PARAMETER_NAME) val name: String,
  @JsonProperty(TYPE) val type: String
) {
  override fun toString() = "${name}$NAME_TYPE_SEPARATOR$type"

  companion object {
    private const val NAME_TYPE_SEPARATOR = ": "
  }
}

enum class SignatureSource {
  HIDDEN_FILE,
  VISIBLE_FILE,
  MODEL_SOLUTION,
  GENERATED_SOLUTION
}

object FunctionSignatureJsonNames {
  const val SIGNATURE_NAME = "name"
  const val PARAMETERS = "parameters"
  const val RETURN_TYPE = "returnType"
  const val SIGNATURE_SOURCE = "signatureSource"
  const val BODY_LINE_COUNT = "bodyLineCount"
}

object FunctionParameterJsonNames {
  const val PARAMETER_NAME = "name"
  const val TYPE = "type"
}