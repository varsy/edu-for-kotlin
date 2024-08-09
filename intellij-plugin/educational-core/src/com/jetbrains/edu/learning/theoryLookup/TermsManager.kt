package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createTopic
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.educational.ml.theory.lookup.core.TermsProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Searches for terms in a task using the ml library and stores a map that stores a list of terms for each task.
 */
@Service(Service.Level.PROJECT)
class TermsManager(private val project: Project) {

  /**
   * Represents a map of terms (value and definition) for each task that may be of potential interest to the user.
   */
  private val terms = ConcurrentHashMap<Int, Map<String, String>>()

  suspend fun extractTerms(task: Task) {
    if (terms.containsKey(task.id)) return
    val text = runReadAction {
      task.getDescriptionFile(project)?.getTextFromTaskTextFile()
    } ?: return
    val termsProvider = TermsProvider()
    val termsList = termsProvider.findTermsAndDefinitions(text)
    termsList.getOrThrow().takeIf { it.isNotEmpty() }?.let {
      terms.computeIfAbsent(task.id) { _ ->
        Lemmatizer(text, it).getTermsAndItsDefinitions()
      }
      notifyTermsChanged(task)
    }
  }

  fun getTerms(task: Task) = terms.getOrDefault(task.id, emptyMap())

  private fun notifyTermsChanged(task: Task) {
    project.messageBus.syncPublisher(TOPIC).termsChanged(task)
  }

  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<TermsListener> = createTopic("Edu.terms")

    fun getInstance(project: Project): TermsManager {
      return project.service()
    }
  }
}

fun interface TermsListener {
  fun termsChanged(task: Task)
}
