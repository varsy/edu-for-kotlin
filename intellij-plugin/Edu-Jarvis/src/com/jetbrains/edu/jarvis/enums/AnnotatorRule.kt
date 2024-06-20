package com.jetbrains.edu.jarvis.enums

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.callSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.functionSynonyms

enum class AnnotatorRule(val regex: Regex) {
  ISOLATED_CODE("`([^`]+)`".toRegex()),
  NO_PARENTHESES_FUNCTION_CALL(
    ("(?i)(?:${callSynonyms.joinToString("|")})"+
     "(?:\\s+the)?(?:\\s+(?:${functionSynonyms.joinToString("|")}))?(?-i)\\s+`([A-Za-z][A-Za-z0-9]+)`").toRegex()
  )
}