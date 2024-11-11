package com.jetbrains.edu.cognifire.utils

import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

object RedundantTodoCleaner {
  fun deleteWrongTodo(promptToCodeContent: PromptToCodeContent, functionSignature: FunctionSignature): PromptToCodeContent {
    if (functionSignature.returnType != UNIT_RETURN_VALUE) return promptToCodeContent
    val lastCodeLine = promptToCodeContent.maxOfOrNull { it.codeLineNumber } ?: return promptToCodeContent
    val todoSpecifyReturnValueRegex = "(?i).*TODO\\(\\s*\".*specify the return value.*\"\\s*\\).*".toRegex()
    return promptToCodeContent
      .filterNot { todoSpecifyReturnValueRegex.containsMatchIn(it.generatedCodeLine) && it.codeLineNumber == lastCodeLine }
  }
}
