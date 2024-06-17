package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.errors.AnnotatorError

data class DescriptionAnnotatorResult(val range: IntRange, val error: AnnotatorError)
