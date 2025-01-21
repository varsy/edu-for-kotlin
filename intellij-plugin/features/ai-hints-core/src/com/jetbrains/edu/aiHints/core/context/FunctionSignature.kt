package com.jetbrains.edu.aiHints.core.context

data class FunctionSignature(
  val name: String,
  val parameters: List<FunctionParameter>,
  val returnType: String?,
  val signatureSource: SignatureSource? = null,
  val bodyLineCount: Int? = null
) {
  override fun toString(): String {
    val signaturePrefix = "$name(${parameters.joinToString(separator = ", ")})"
    if (returnType == null) return signaturePrefix
    return "$signaturePrefix: $returnType"
  }

  /**
   * The equality check doesn't cover all properties of an object, which is done on purpose.
   * For example, in [com.jetbrains.edu.aiHints.core.TaskProcessorImpl.getShortFunctionFromSolutionIfRecommended] we search for the generated function,
   * which might have a different implementation. That is, we only want to compare by function signature, which is name and parameters.
   */
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
}