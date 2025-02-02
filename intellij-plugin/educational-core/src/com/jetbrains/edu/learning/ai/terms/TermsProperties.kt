package com.jetbrains.edu.learning.ai.terms

import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion

data class TermsProperties(
  val language: TranslationLanguage,
  val terms: Map<Int, List<Term>>,
  val version: TermsVersion
)