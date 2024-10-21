package com.jetbrains.edu.cognifire.utils

import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

object RedundantTodoCleaner {
  fun deleteWrongTodo(promptToCode: PromptToCodeResponse, functionSignature: FunctionSignature): PromptToCodeResponse {
    if (functionSignature.returnType != UNIT_RETURN_VALUE) return promptToCode
    val lastCodeLine = promptToCode.maxOfOrNull { it.codeLineNumber } ?: return promptToCode
    val todoSpecifyReturnValueRegex = "(?i).*TODO\\(\\s*\".*specify the return value.*\"\\s*\\).*".toRegex()
    return promptToCode.filterNot { todoSpecifyReturnValueRegex.containsMatchIn(it.generatedCodeLine) && it.codeLineNumber == lastCodeLine }
  }
}
