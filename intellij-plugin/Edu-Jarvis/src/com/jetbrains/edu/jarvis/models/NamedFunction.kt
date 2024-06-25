package com.jetbrains.edu.jarvis.models

data class NamedFunction(override val name:String, val numberOfArguments: Int) : NamedEntity {

  companion object {

    val namedFunctionRegex = "[a-zA-Z_][a-zA-Z0-9_]*\\((?:\\s*[^(),\\s]+\\s*(?:,\\s*[^(),\\s]+\\s*)*)?\\s*\\)".toRegex()

    fun String.isNamedFunction() = namedFunctionRegex.matches(this)

  }
}
