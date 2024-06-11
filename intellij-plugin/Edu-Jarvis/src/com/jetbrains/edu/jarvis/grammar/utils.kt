package com.jetbrains.edu.jarvis.grammar

import com.jetbrains.edu.jarvis.grammar.generated.ProblemSolvingLanguageBaseListener
import com.jetbrains.edu.jarvis.grammar.generated.ProblemSolvingLanguageLexer
import com.jetbrains.edu.jarvis.grammar.generated.ProblemSolvingLanguageParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

/**
 * Parses the input string by splitting it into sentences and using a custom lexer and parser.
 *
 * @param input The student prompt from the description block.
 * @see ProblemSolvingLanguageBaseListener
 */
fun parse(input: String) {
  val sentences = input.lines().joinToString("").lowercase().split(DOT)
  for (sentence in sentences) {
    val lexer = ProblemSolvingLanguageLexer(CharStreams.fromString(sentence))
    val tokens: TokenStream = CommonTokenStream(lexer)
    val parser = ProblemSolvingLanguageParser(tokens)
    val result = parser.sentence()
    val walker = ParseTreeWalker()
    val listener = ProblemSolvingLanguageBaseListener()
    walker.walk(listener, result)
  }
}

private const val DOT = "."
