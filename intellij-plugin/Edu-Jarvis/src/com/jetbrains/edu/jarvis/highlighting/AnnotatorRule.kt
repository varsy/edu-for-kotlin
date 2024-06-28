package com.jetbrains.edu.jarvis.highlighting

import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.callFunction
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.createVariable
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.isolatedCode
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.saveVariable
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.setVariable
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.storeVariable

/**
 * Represents a rule based on which the [AnnotatorError] can be found.
 * These rules are dictated with the regular expressions.
 */
enum class AnnotatorRule(val regex: Regex) {
  STORE_VARIABLE(storeVariable),
  CREATE_VARIABLE(createVariable),
  SET_VARIABLE(setVariable),
  SAVE_VARIABLE(saveVariable),
  CALL_FUNCTION(callFunction),
  ISOLATED_CODE(isolatedCode),
}