package com.jetbrains.edu.jarvis.highlighting

/**
 * Represents a match based on the [AnnotatorRule].
 * @property rule the associated [AnnotatorRule].
 * @property identifier the [MatchGroup] representing the matched identifier (e.g., function name).
 * @property arguments the substring with function arguments.
 * Can be `null` if there is no match with the `rule`.
 */
data class AnnotatorRuleMatch(val rule: AnnotatorRule, val identifier: MatchGroup, val arguments: String?)
