package com.jetbrains.edu.cognifire.models

data class FunctionSignature(
  val name: String,
  val functionParameters: List<FunctionArgument>,
  val returnType: String
) {
  override fun toString(): String = toString(name)

  fun toStringWithPlaceHolder(): String = toString(PLACEHOLDER)

  fun getNonFqName(): String = name.slice(name.lastIndexOf (".")+1 until name.lastIndex+1)

  private fun toString(name: String): String {
    val parameterListString = functionParameters.joinToString(ARGUMENT_SEPARATOR) { param ->
      "${param.name}: ${param.type}"
    }
    return "fun $name ($parameterListString): $returnType"
  }

  companion object {
    private const val ARGUMENT_SEPARATOR = ", "
    const val PLACEHOLDER = "foo618_cognifire_placeholder_name"
  }
}
