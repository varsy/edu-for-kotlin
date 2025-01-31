package com.jetbrains.edu.learning.theoryLookup

import com.jetbrains.educational.terms.format.Term

//TODO(add terms version)
data class TheoryLookupProperties(
  val terms: Map<Int, List<Term>>
)