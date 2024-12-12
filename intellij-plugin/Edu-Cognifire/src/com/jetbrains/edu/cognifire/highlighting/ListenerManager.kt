package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.selectedEditor
import java.util.*

/**
 * This class manages the event listeners for a given project.
 *
 * @property project The project associated with the event listeners.
 */
@Service(Service.Level.PROJECT)
class ListenerManager(private val project: Project) {
  private val listeners = mutableMapOf<String, MutableList<EventListener>>()

  private val editor: Editor?
    get() = project.selectedEditor

  fun addListener(
    listener: EventListener,
    id: String
  ) {
    when (listener) {
      is EditorMouseMotionListener -> editor?.addEditorMouseMotionListener(listener)
      is DocumentListener -> editor?.document?.addDocumentListener(listener)
    }
    listeners.getOrPut(id) { mutableListOf() }.add(listener)
  }

  private fun removeListener(listener: EventListener) {
    when (listener) {
      is EditorMouseMotionListener -> editor?.removeEditorMouseMotionListener(listener)
      is DocumentListener -> editor?.document?.removeDocumentListener(listener)
    }
  }

  fun clearAll(id: String) {
    listeners[id]?.forEach {
      removeListener(it)
    }
    listeners[id]?.clear()
  }

  fun clearAllMouseMotionListeners(id: String) {
    listeners[id]?.filterIsInstance<EditorMouseMotionListener>()?.forEach {
      editor?.removeEditorMouseMotionListener(it)
    }
    listeners[id]?.removeAll { it is EditorMouseMotionListener }
  }

  companion object {
    fun getInstance(project: Project): ListenerManager = project.service()
  }
}
