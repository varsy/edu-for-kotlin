package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.jetbrains.edu.cognifire.models.ProdeExpression
import com.jetbrains.edu.cognifire.ui.ProdeReadonlyFragmentHandler

@State(name = "GuardedBlockManager", storages = [Storage("storage.xml")])
class GuardedBlockManager : PersistentStateComponent<GuardedBlockManager> {
  private val blocks = mutableMapOf<String, RangeMarker>()

  fun removeGuardedBlock(id: String, document: Document) {
    blocks[id]?.let { range -> document.removeGuardedBlock(range) }
    blocks.remove(id)
  }

  fun addGuardedBlock(document: Document, startOffset: Int, endOffset: Int, id: String) {
    blocks[id]?.let { range -> document.removeGuardedBlock(range) }
    val block = document.createGuardedBlock(startOffset, endOffset)
    blocks[id] = block
  }

  fun setReadonlyFragmentModificationHandler(document: Document, prode: ProdeExpression) {
    val editor = EditorFactory.getInstance().getEditors(document).firstOrNull() ?: return
    EditorActionManager.getInstance()
      .setReadonlyFragmentModificationHandler(document, ProdeReadonlyFragmentHandler(editor, prode))
  }

  override fun getState() = this

  override fun loadState(state: GuardedBlockManager) {
    blocks.putAll(state.blocks)
  }

  companion object {
    fun getInstance(): GuardedBlockManager = service()
  }
}