package com.jetbrains.edu.cognifire.highlighting.undefinedidentifier

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.callFunction
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.createVariable
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.loopExpression
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.isolatedCode
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.saveVariable
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.setVariable
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.storeVariable

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
  LOOP_EXPRESSION(loopExpression),
  ISOLATED_CODE(isolatedCode),
}
