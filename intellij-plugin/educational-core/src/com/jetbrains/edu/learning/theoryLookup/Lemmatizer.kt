package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.educational.ml.theory.lookup.term.Term
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import org.languagetool.language.AmericanEnglish

/**
 * Generates a map from the original terms in a provided string to their respective lemmas.
 *
 * The map construction involves several steps:
 * 1) all terms are lemmatized using Stanford's CoreNLP library for natural language processing and lowercase;
 * 2) the original text is tokenized into separate word;
 * 3) each lemmatized term is then compared with the lemmatized text to find an exact location of the term in the text;
 * 4) the original term and its lemmatized version are put into a map.
 */
@JvmInline
value class Lemmatizer(private val termToDefinitionAndLemmaMap: MutableMap<String, Pair<String, String>>) {

  constructor(text: String, terms: List<Term>) : this(mutableMapOf()) {
    try {
      val languageTool = JLanguageTool(AmericanEnglish())
      val lemmatizedTerms = terms.map { term -> Pair(term.value.textToTokens(languageTool).map { it.lemmatizeToken() }, term.definition) }

      val tokens = text.textToTokens(languageTool)
      val originalTokens = tokens.map { it.token }
      val lemmatizedText = tokens.joinToString(SEPARATOR) { it.lemmatizeToken() }

      lemmatizedTerms.forEach { (term, definition) ->
        // terms may consist of several tokens
        val joinedTerm = term.joinToString(SEPARATOR)
        // find all occurrences of the term in the text
        lemmatizedText.indicesOf(joinedTerm) { index ->
          // find index of the first token in the text
          val indexOfTerm = if (index == 0) 0 else lemmatizedText.substring(0, index).tokenCount(languageTool)
          if (indexOfTerm + term.size < originalTokens.size) {
            // by index of the first token and number of tokens in the term find the original version of the term in the text
            val originalTerm = originalTokens.subList(indexOfTerm, indexOfTerm + term.size).joinToString(SEPARATOR)
            // the original term and its lemmatized version are put into a map
            termToDefinitionAndLemmaMap[originalTerm] = Pair(definition, joinedTerm)
          }
        }
      }
    } catch (e: Exception) {
      LOG.error("Failed to build lemmatized terms map for text $text and terms: $terms", e)
    }
  }

  fun getTermsAndItsDefinitions(): Map<String, String> = termToDefinitionAndLemmaMap.mapValues { it.value.first }

  fun getLemmatizedTerms(): Map<String, String> = termToDefinitionAndLemmaMap.mapValues { it.value.second }

  private fun AnalyzedTokenReadings.lemmatizeToken() = readings.last().lemma?.lowercase() ?: token

  private fun String.textToTokens(languageTool: JLanguageTool) =
    languageTool.analyzeText(this).map { sentence ->
      sentence.tokens.filterNot { it.isSentenceStart }.toList() }.flatten()

  private fun String.indicesOf(substring: String, onEachIndex: (Int) -> Unit) {
    var index = indexOf(substring, ignoreCase = true)
    while (index != -1) {
      onEachIndex(index)
      index = indexOf(substring, index + 1, ignoreCase = true)
    }
  }

  private fun String.tokenCount(languageTool: JLanguageTool) = textToTokens(languageTool).map { it.token }.count()

  companion object {
    private const val SEPARATOR = ""
    private val LOG: Logger = Logger.getInstance(Lemmatizer::class.java)
  }
}
