package com.jetbrains.edu.cognifire.highlighting.undefinedidentifier

/**
 * Represents a match based on the [AnnotatorRule].
 * @property rule the associated [AnnotatorRule].
 * @property identifier the [MatchGroup] representing the matched identifier (e.g., function name).
 * @property arguments the substring with function arguments.
 * @property keywords representing matching keywords (e.g., create, set, store)
 * @property values representing matching values (e.g., 3, true)
 * @property strings representing matching strings (e.g., "Hello", "string")
 * Can be `null` if there is no match with the `rule`.
 */
data class AnnotatorRuleMatch(
  val rule: AnnotatorRule,
  val identifier: MatchGroup,
  val arguments: String? = null,
  val keywords: List<MatchGroup> = emptyList(),
  val values: List<MatchGroup> = emptyList(),
  val strings: List<MatchGroup> = emptyList()
)
