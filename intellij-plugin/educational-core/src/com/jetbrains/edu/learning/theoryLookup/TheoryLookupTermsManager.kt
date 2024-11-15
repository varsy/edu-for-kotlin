package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.LightTestAware
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createTopic
import com.jetbrains.educational.ml.theory.lookup.term.Term
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
@State(name = "TheoryLookupTermsStorage", storages = [Storage("theory_lookup.xml", roamingType = RoamingType.DISABLED)])
class TheoryLookupTermsManager(private val project: Project) : SimplePersistentStateComponent<TheoryLookupTermsManager.TermsState>(TermsState()), LightTestAware {
  fun getTaskTerms(task: Task): List<Term>? {
    val taskKey = task.id
    return state.taskTerms[taskKey]?.map { Term(it.key, it.value) }
  }

  fun updateTaskTerms(taskToTerms: Map<Task, List<Term>>) {
    for ((task, terms) in taskToTerms) {
      setTaskTerms(task, terms)
    }
  }

  @VisibleForTesting
  fun setTaskTerms(task: Task, terms: List<Term>) {
    val taskKey = task.id
    state.taskTerms[taskKey] = terms.associate { it.value to it.definition }
    notifyTermsChanged(task)
  }

  fun shouldUpdateCourseTerms(course: Course): Boolean {
    return course.allTasks.any { !hasTerms(it) }
  }

  private fun hasTerms(task: Task): Boolean = state.taskTerms[task.id] != null

  override fun cleanUpState() {
    state.taskTerms.clear()
  }

  private fun notifyTermsChanged(task: Task) {
    project.messageBus.syncPublisher(TOPIC).termsChanged(task)
  }

  class TermsState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val taskTerms: MutableMap<Int, Map<String, String>> by map()
  }

  fun interface TermsListener {
    fun termsChanged(task: Task)
  }

  companion object {
    fun getInstance(project: Project): TheoryLookupTermsManager = project.service()

    @Topic.ProjectLevel
    val TOPIC: Topic<TermsListener> = createTopic("Edu.terms")
  }
}