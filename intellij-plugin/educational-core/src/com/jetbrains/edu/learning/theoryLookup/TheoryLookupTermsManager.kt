package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.terms.format.Term
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
@State(name = "TheoryLookupTermsStorage", reloadable = true, storages = [Storage("theory_lookup.xml", roamingType = RoamingType.DISABLED)])
class TheoryLookupTermsManager : PersistentStateComponent<TheoryLookupTermsManager.TermsState>, EduTestAware {
  private val _theoryLookupProperties = MutableStateFlow<TheoryLookupProperties?>(null)
  val theoryLookupProperties = _theoryLookupProperties.asStateFlow()

  fun getTaskTerms(task: Task): List<Term>? = theoryLookupProperties.value?.terms?.get(task.id)

  fun setTheoryLookupProperties(properties: TheoryLookupProperties?) {
    _theoryLookupProperties.value = properties
  }

  @VisibleForTesting
  fun setTaskTerms(tasksToTerms: Map<Task, List<Term>>) {
    val properties = TheoryLookupProperties(tasksToTerms.entries.associate { it.key.id to it.value })
    setTheoryLookupProperties(properties)
  }

  fun areCourseTermsLoaded(): Boolean {
    return _theoryLookupProperties.value != null
  }

  override fun cleanUpState() {
    _theoryLookupProperties.value = null
  }

  override fun getState(): TermsState {
    val state = TermsState()
    val properties = _theoryLookupProperties.value ?: return state
    state.taskTerms = properties.terms.entries.associateTo(mutableMapOf()) { (taskId, terms) ->
      taskId to terms.map { it.asStoredTerm() }
    }
    return state
  }

  override fun loadState(state: TermsState) {
    val taskTerms = mutableMapOf<Int, List<Term>>()
    for ((taskId, terms) in state.taskTerms) {
      taskTerms[taskId] = terms.map { it.asTerm() }
    }

    _theoryLookupProperties.value = TheoryLookupProperties(taskTerms)
  }

  @Tag("Term")
  data class StoredTerm(
    @Attribute var value: String,
    @Attribute var definition: String
  ) {
    fun asTerm(): Term = Term(value, definition)
  }

  private fun Term.asStoredTerm(): StoredTerm = StoredTerm(value, definition)

  class TermsState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var taskTerms by map<Int, List<StoredTerm>>()
  }

  companion object {
    fun getInstance(project: Project): TheoryLookupTermsManager = project.service()
  }
}