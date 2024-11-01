package com.jetbrains.edu.aiHints.core.generator

import com.jetbrains.edu.aiHints.core.connector.HintsServiceConnector
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.generator.TextHintGenerator
import com.jetbrains.educational.ml.hints.hint.TextHint

class AiTextHintGenerator : TextHintGenerator() {
  override suspend fun queryHint(context: TextHintContext): TextHint {
    return HintsServiceConnector.getInstance().getTextHint(context)
  }
}