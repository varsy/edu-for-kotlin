package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createTopic
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.theory.lookup.core.TermsProvider
import com.jetbrains.educational.ml.theory.lookup.term.Term
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Searches for terms in a task using the ml library and stores a map that stores a list of terms for each task.
 */
@Service(Service.Level.PROJECT)
class TermsManager(private val project: Project) : CoroutineScope {

  private val job = SupervisorJob()

  private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
    EduNotificationManager.showErrorNotification(
      project,
      EduCoreBundle.message("theory.lookup.notification.failed.to.extract.terms.title"),
      exception.message ?:
      EduCoreBundle.message("theory.lookup.notification.failed.to.extract.terms.text")
    )
    thisLogger().error("Terms extracting failed", exception)
  }

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + job + exceptionHandler

  private val termsStorage
    get() = TheoryLookupTermsStorage.getInstance(project)

  suspend fun extractTerms(task: Task) {
    if (termsStorage.hasTerms(task)) return
    val text = runReadAction {
      task.getDescriptionFile(project)?.getTextFromTaskTextFile()
    } ?: return
    val termsProvider = TermsProvider()
    val termsList = termsProvider.findTermsAndDefinitions(text).getOrThrow()

    // TODO(get rid of the lemmatizer in next review)
    val lemmatizedTerms = Lemmatizer(text, termsList).getTermsAndItsDefinitions().map { Term(it.key, it.value) }

    termsStorage.setTaskTerms(task, lemmatizedTerms)
    notifyTermsChanged(task)
  }

  fun getTerms(task: Task) = termsStorage.getTaskTerms(task) ?: emptyList()

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
