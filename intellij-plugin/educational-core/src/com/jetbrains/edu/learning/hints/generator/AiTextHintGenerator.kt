package com.jetbrains.edu.learning.hints.generator

import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.generator.TextHintGenerator
import com.jetbrains.educational.ml.hints.hint.HintOutput
import com.jetbrains.educational.ml.hints.hint.TextHint
import com.jetbrains.educational.ml.hints.prompt.TextHintPrompt

class AiTextHintGenerator : TextHintGenerator() {
  override suspend fun queryHint(context: TextHintContext): HintOutput<TextHint, TextHintPrompt> {
    TODO("Not yet implemented")
  }
}