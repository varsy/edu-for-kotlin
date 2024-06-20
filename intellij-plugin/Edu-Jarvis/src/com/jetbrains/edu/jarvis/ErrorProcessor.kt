package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError

/**
 * Returns an [[AnnotatorParametrizedError]] associated with relevant context.
 */
interface ErrorProcessor {
  /**
   * Processes the case with isolated code blocks.
   */
  fun processIsolatedCode(): AnnotatorParametrizedError
  /**
   * Processes the case with function calls without parentheses.
   */
  fun processNoParenthesesFunctionCall(): AnnotatorParametrizedError
}
