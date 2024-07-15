package com.jetbrains.edu.jarvis.grammar

import com.jetbrains.edu.jarvis.grammar.generated.ProblemSolvingLanguageLexer
import com.jetbrains.edu.jarvis.grammar.generated.ProblemSolvingLanguageParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStream

/**
 * Parses the input string by splitting it into sentences and using a custom lexer and parser.
 *
 * @param input The student prompt from the description block.
 */
fun parse(input: String) {
  input.lines()
    .joinToString("")
    .lowercase()
    .split(DOT)
    .filter { it.isNotBlank() }
    .forEach { parseSentence(it) }
}

fun parseSentence(sentence: String): ProblemSolvingLanguageParser.SentenceContext? {
  val lexer = ProblemSolvingLanguageLexer(CharStreams.fromString(sentence))
  val tokens: TokenStream = CommonTokenStream(lexer)
  val parser = ProblemSolvingLanguageParser(tokens)
  parser.addErrorListener(ThrowingErrorListener.INSTANCE)
  return parser.sentence()
}

private const val DOT = "."
