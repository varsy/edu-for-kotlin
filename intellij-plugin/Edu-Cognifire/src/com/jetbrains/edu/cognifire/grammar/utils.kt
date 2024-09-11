package com.jetbrains.edu.cognifire.grammar

import com.jetbrains.edu.cognifire.grammar.generated.ProblemSolvingLanguageLexer
import com.jetbrains.edu.cognifire.grammar.generated.ProblemSolvingLanguageParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStream

/**
 * Parses the string using a custom lexer and parser.
 */

fun String.parse(): ProblemSolvingLanguageParser.SentenceContext {
  val lexer = ProblemSolvingLanguageLexer(CharStreams.fromString(this.lowercase()))
  val tokens: TokenStream = CommonTokenStream(lexer)
  val parser = ProblemSolvingLanguageParser(tokens)
  parser.addErrorListener(ThrowingErrorListener.INSTANCE)
  return parser.sentence()
}

