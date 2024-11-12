package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.LightTestAware
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.theory.lookup.term.Term

@Service(Service.Level.PROJECT)
@State(name = "TheoryLookupTermsStorage", storages = [Storage("theory_lookup.xml", roamingType = RoamingType.DISABLED)])
class TheoryLookupTermsStorage : SimplePersistentStateComponent<TheoryLookupTermsStorage.TermsState>(TermsState()), LightTestAware {
  fun getTaskTerms(task: Task): List<Term>? {
    val taskKey = task.pathInCourse
    return state[taskKey]?.map { Term(it.key, it.value) }
  }

  fun setTaskTerms(task: Task, terms: List<Term>) {
    val taskKey = task.pathInCourse
    state[taskKey] = terms.associate { it.value to it.definition }
  }

  fun hasTerms(task: Task): Boolean = state[task.pathInCourse] != null

  override fun cleanUpState() {
    state.clear()
  }

  class TermsState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val taskTerms: MutableMap<String, Map<String, String>> by map()

    operator fun get(key: String): Map<String, String>? = taskTerms[key]

    operator fun set(key: String, terms: Map<String, String>) {
      incrementModificationCount()
      taskTerms[key] = terms
    }

    fun clear() {
      incrementModificationCount()
      taskTerms.clear()
    }
  }

  companion object {
    fun getInstance(project: Project): TheoryLookupTermsStorage = project.service()
  }
}