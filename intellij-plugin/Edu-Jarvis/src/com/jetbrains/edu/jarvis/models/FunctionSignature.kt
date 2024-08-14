package com.jetbrains.edu.jarvis.models

data class FunctionSignature(
  val name: String,
  val functionParameters: List<FunctionArgument>,
  val returnType: String
) {
  override fun toString(): String {
    val parameterListString = functionParameters.joinToString(ARGUMENT_SEPARATOR) { param ->
      "${param.name}: ${param.type}"
    }
    return "fun $name ($parameterListString): $returnType"
  }

  companion object {
    private const val ARGUMENT_SEPARATOR = ", "
  }
}