package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.actionSystem.ActionToolbar
import org.apache.commons.lang.text.StrSubstitutor

// BACKCOMPAT: 2023.2. Inline it.
fun replaceWithTemplateText(resources: Map<String, String>, templateText: String): String {
  return StrSubstitutor(resources).replace(templateText)
}

// BACKCOMPAT: 2023.3. Inline it.
fun ActionToolbar.setupLayoutStrategy() {
  layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
}
