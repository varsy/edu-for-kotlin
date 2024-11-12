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
    val taskKey = task.id
    return state.taskTerms[taskKey]?.map { Term(it.key, it.value) }
  }

  fun setTaskTerms(task: Task, terms: List<Term>) {
    val taskKey = task.id
    state.taskTerms[taskKey] = terms.associate { it.value to it.definition }
  }

  fun hasTerms(task: Task): Boolean = state.taskTerms[task.id] != null

  override fun cleanUpState() {
    state.taskTerms.clear()
  }

  class TermsState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val taskTerms: MutableMap<Int, Map<String, String>> by map()
  }

  companion object {
    fun getInstance(project: Project): TheoryLookupTermsStorage = project.service()
  }
}