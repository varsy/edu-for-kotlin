package com.jetbrains.edu.learning.ai.terms

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings.TermsProjectState
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator
import kotlin.collections.set

@Service(Service.Level.PROJECT)
@State(name = "TermsProjectSettings", reloadable = true, storages = [Storage("edu_terms.xml", roamingType = RoamingType.DISABLED)])
class TermsProjectSettings : PersistentStateComponent<TermsProjectState>, EduTestAware {
  private val _termsProperties = MutableStateFlow<TermsProperties?>(null)
  val termsProperties = _termsProperties.asStateFlow()
  private val versionsByLanguage = ConcurrentHashMap<TranslationLanguage, TermsVersion>()
  private val termsByLanguage = ConcurrentHashMap<TranslationLanguage, Map<Int, List<Term>>>()

  fun getTaskTerms(task: Task): List<Term>? = termsProperties.value?.terms?.get(task.id)

  fun setTerms(termsProperties: TermsProperties?) {
    if (termsProperties == null) {
      _termsProperties.value = null
      return
    }
    val language = termsProperties.language
    termsByLanguage[language] = termsProperties.terms
    versionsByLanguage[language] = termsProperties.version
    _termsProperties.value = termsProperties
  }

  fun getTermsByLanguage(language: TranslationLanguage): TermsProperties? {
    val terms = termsByLanguage[language] ?: return null
    val version = versionsByLanguage[language] ?: return null
    return TermsProperties(language, terms, version)
  }

  override fun getState(): TermsProjectState {
    val state = TermsProjectState()
    state.terms = termsByLanguage.mapValuesTo(mutableMapOf()) { (_, terms) ->
      terms.mapValues { (_, taskTerms) ->
        taskTerms.map { it.asStoredTerm() }
      }
    }
    state.termsVersions = versionsByLanguage.mapValuesTo(mutableMapOf()) { it.value.value }
    state.currentTermsLanguage = termsProperties.value?.language
    return state
  }

  override fun loadState(state: TermsProjectState) {
    termsByLanguage.clear()
    for ((language, terms) in state.terms) {
      val taskTerms = mutableMapOf<Int, List<Term>>()
      for ((taskId, storedTaskTerms) in terms) {
        taskTerms[taskId] = storedTaskTerms.map { it.asTerm() }
      }
      termsByLanguage[language] = taskTerms
    }
    versionsByLanguage.clear()
    for ((language, version) in state.termsVersions) {
      versionsByLanguage[language] = TermsVersion(version)
    }

    val language = state.currentTermsLanguage ?: return
    _termsProperties.value = getTermsByLanguage(language)
  }

  fun resetTerms() {
    _termsProperties.value = null
    termsByLanguage.clear()
    versionsByLanguage.clear()
  }

  @TestOnly
  override fun cleanUpState() {
    resetTerms()
  }

  @Tag("Term")
  data class StoredTerm(
    @Attribute var value: String,
    @Attribute var definition: String
  ) {
    fun asTerm(): Term = Term(value, definition)
  }

  private fun Term.asStoredTerm(): StoredTerm = StoredTerm(value, definition)

  class TermsProjectState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var currentTermsLanguage by enum<TranslationLanguage>()

    @get:XCollection(style = XCollection.Style.v2)
    var terms by map<TranslationLanguage, Map<Int, List<StoredTerm>>>()

    @get:XCollection(style = XCollection.Style.v2)
    var termsVersions by map<TranslationLanguage, Int>()
  }

  companion object {
    fun getInstance(project: Project): TermsProjectSettings = project.service()

    fun areCourseTermsLoaded(project: Project): Boolean = getInstance(project).termsProperties.value != null
  }
}