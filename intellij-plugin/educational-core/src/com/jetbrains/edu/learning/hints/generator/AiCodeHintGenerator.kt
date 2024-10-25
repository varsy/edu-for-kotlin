package com.jetbrains.edu.learning.hints.generator

import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.generator.CodeHintGenerator
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.HintOutput
import com.jetbrains.educational.ml.hints.processors.TaskProcessor
import com.jetbrains.educational.ml.hints.prompt.CodeHintPrompt

class AiCodeHintGenerator(taskProcessor: TaskProcessor) : CodeHintGenerator(taskProcessor) {
  override suspend fun queryHint(context: CodeHintContext): HintOutput<CodeHint, CodeHintPrompt> {
    TODO("Not yet implemented")
  }
}