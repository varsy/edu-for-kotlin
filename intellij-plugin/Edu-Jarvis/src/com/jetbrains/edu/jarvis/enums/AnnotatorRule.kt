package com.jetbrains.edu.jarvis.enums

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.callSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.declareSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.functionSynonyms

enum class AnnotatorRule(val regex: Regex) {
  VARIABLE_DECLARATION(
    "(?i)(?:${declareSynonyms.joinToString("|")})(?:\\s+the)?(?-i)\\s+`([a-zA-Z_][a-zA-Z0-9_]*)`".toRegex()
  ),
  NO_PARENTHESES_FUNCTION(
    ("(?i)(?:${callSynonyms.joinToString("|")})" +
     "(?:\\s+the)?(?:\\s+(?:${functionSynonyms.joinToString("|")}))?(?-i)\\s+`([A-Za-z][A-Za-z0-9]+)`").toRegex()
  ),
  ISOLATED_CODE("`([^`]+)`".toRegex()),
}