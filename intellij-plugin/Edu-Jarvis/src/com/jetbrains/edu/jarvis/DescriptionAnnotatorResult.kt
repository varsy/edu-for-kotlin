package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError

data class DescriptionAnnotatorResult(val range: IntRange, val parametrizedError: AnnotatorParametrizedError)
