package com.jetbrains.edu.jarvis.models

data class NamedVariable(override val name: String) : NamedEntity {

  companion object {

    private val namedVariableRegex = "[a-zA-Z_][a-zA-Z0-9_]*\\((?:\\s*[^(),\\s]+\\s*(?:,\\s*[^(),\\s]+\\s*)*)?\\s*\\)".toRegex()

    fun String.isNamedVariable() = namedVariableRegex.matches(this)

  }
}

