package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError

/**
 * Returns an [[AnnotatorParametrizedError]] associated with relevant context.
 */
interface ErrorProcessor {
  /**
   * Processes a named function.
   */
  fun processNamedFunction(): AnnotatorParametrizedError
  /**
   * Processes a named variable.
   */
  fun processNamedVariable(): AnnotatorParametrizedError
}
