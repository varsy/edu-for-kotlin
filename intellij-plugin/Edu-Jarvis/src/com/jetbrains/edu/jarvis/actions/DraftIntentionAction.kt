package com.jetbrains.edu.jarvis.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import javax.swing.Icon

class DraftIntentionAction : IntentionAction, Iconable {
  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String = EduJarvisBundle.message("action.draft.intention.family.name")

  override fun getText(): String = EduJarvisBundle.message("action.draft.intention.text")

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    // TODO: verify that there are no errors
    val caretModel: CaretModel = editor?.caretModel ?: return false
    val element: PsiElement = file?.findElementAt(caretModel.offset) ?: return false
    return element.text == DRAFT
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    // TODO: replace description with draft
  }

  override fun getIcon(flags: Int): Icon = AllIcons.Actions.IntentionBulb

  companion object {
    private const val DRAFT = "draft"
  }
}
