package com.jetbrains.edu.jarvis.enums

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.callSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.declareSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.functionSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.variableSynonyms

enum class AnnotatorRule(val regex: Regex) {
  VARIABLE_DECLARATION(
    ("(?i)(?:${declareSynonyms().joinToString("|")})" +
     "(?:\\s+the)?(?:\\s+(?:${variableSynonyms().joinToString("|")}))?(?-i)\\s+`([a-zA-Z_][a-zA-Z0-9_]*)`").toRegex()
  ),
  NO_PARENTHESES_FUNCTION(
    ("(?i)(?:${callSynonyms().joinToString("|")})" +
     "(?:\\s+the)?(?:\\s+(?:${functionSynonyms().joinToString("|")}))?(?-i)\\s+`([a-zA-Z_][a-zA-Z0-9_]*)`").toRegex()
  ),
  ISOLATED_CODE("`([^`]+)`".toRegex()),
}