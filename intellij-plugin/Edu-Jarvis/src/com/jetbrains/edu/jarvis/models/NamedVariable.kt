package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRuleMatch

data class NamedVariable(override val name: String) : NamedEntity {
  constructor(target: AnnotatorRuleMatch)
    : this(target.identifier.value)
}
