package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.*

@State(name = "ProdeStateManager", storages = [Storage("storage.xml")])
class ProdeStateManager : PersistentStateComponent<ProdeStateManager> {
  private val hasMouseMotionListener = mutableSetOf<String>()
  private val prodeData = mutableMapOf<String, ListenerData>()

  fun addProde(
    prodeID: String,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>,
    initialPrompt: String,
    initialCode: String
  ) {
    prodeData[prodeID] = ListenerData(
      promptToCodeLines, codeToPromptLines, initialPrompt, initialCode
    )
  }

  fun addMouseMotionListener(prodeId: String) {
    hasMouseMotionListener.add(prodeId)
  }

  fun removeMouseMotionListener(prodeId: String) {
    hasMouseMotionListener.remove(prodeId)
  }

  fun hasMouseMotion(prodeId: String): Boolean  = hasMouseMotionListener.contains(prodeId)

  fun getProdeData(prodeId: String) = prodeData[prodeId]

  override fun getState() = this

  override fun loadState(state: ProdeStateManager) {
    hasMouseMotionListener.addAll(state.hasMouseMotionListener)
    prodeData.putAll(state.prodeData)
  }

  companion object {
    fun getInstance(): ProdeStateManager = service()
  }

  data class ListenerData(
    val promptToCodeLines: Map<Int, List<Int>>,
    val codeToPromptLines: Map<Int, List<Int>>,
    val initialPrompt: String,
    val initialCode: String
  )
}