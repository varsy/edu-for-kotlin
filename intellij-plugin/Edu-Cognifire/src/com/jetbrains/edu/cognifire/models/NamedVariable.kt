package com.jetbrains.edu.cognifire.models

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRuleMatch

data class NamedVariable(override val name: String) : NamedEntity {
  constructor(target: AnnotatorRuleMatch)
    : this(target.identifier.value)
}
