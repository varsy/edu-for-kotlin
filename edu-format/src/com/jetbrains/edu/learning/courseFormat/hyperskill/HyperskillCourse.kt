package com.jetbrains.edu.learning.courseFormat.hyperskill

import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.wrapWithUtm
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {

  constructor()

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  var hyperskillProject: HyperskillProject? = null
    set(value) {
      field = value
      val intId = value?.id ?: 0
      id = "" + intId
    }

  var selectedStage: Int? = null
  var selectedProblem: Int? = null

  constructor(hyperskillProject: HyperskillProject, languageId: String, languageVersion: String?, environment: String) {
    this.hyperskillProject = hyperskillProject
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    this.languageId = languageId
    this.languageVersion = languageVersion
    this.environment = environment
    id = "" + hyperskillProject.id
  }

  constructor(languageName: String, languageId: String, languageVersion: String?) {
    name = "${languageName.capitalize()} Problems"
    description = message("hyperskill.problems.project.description", languageName.capitalize())
    this.languageId = languageId
    this.languageVersion = languageVersion
  }

  val isTemplateBased: Boolean
    get() {
      return (hyperskillProject ?: error("Disconnected $HYPERSKILL project")).isTemplateBased
    }


  fun getProjectLesson(): FrameworkLesson? = lessons.firstOrNull() as? FrameworkLesson

  /**
   * Deprecated. Hyperskill problems are used to be stored in [HYPERSKILL_PROBLEMS] lesson.
   *
   * Structure example:
   *
   * [HYPERSKILL_PROBLEMS] lesson
   *
   *     `Arithmetic average` task
   *
   *     `Thread-safe account` task
   *
   *     `Countdown counter` task
   *
   *    etc
   *
   */
  @Deprecated("Problems lesson isn't used anymore, use Topics section instead", replaceWith = ReplaceWith("getTopicsSection()"))
  fun getProblemsLesson(): Lesson? = getLesson { it.presentableName == HYPERSKILL_PROBLEMS }

  /**
   * Hyperskill problems are grouped by their topics. Topics are lessons located in [HYPERSKILL_TOPICS] section.
   *
   * Structure example:
   *
   * [HYPERSKILL_TOPICS] section
   *
   *    `The for-loop` lesson
   *
   *        `Arithmetic average` task
   *
   *        `Size of parts` task
   *
   *        etc
   *
   *     `Thread synchronization` lesson
   *
   *        `Thread-safe account` task
   *
   *        `Countdown counter` task
   *
   *        etc
   *
   */
  fun getTopicsSection(): Section? = getSection { it.presentableName == HYPERSKILL_TOPICS }

  fun getProblem(id: Int): Task? {
    getTopicsSection()?.lessons?.forEach { lesson ->
      lesson.getTaskById(id)?.let {
        return it
      }
    }
    return null
  }

  fun isTaskInProject(task: Task): Boolean = task.lesson == getProjectLesson()

  fun isTaskInTopicsSection(task: Task): Boolean = getTopicsSection()?.lessons?.contains(task.lesson) == true

  private fun descriptionNote(projectId: Int): String {
    val link = "$HYPERSKILL_PROJECTS_URL/$projectId"
    return """<br/><br/>${message("learn.more.at")} <a href="${wrapWithUtm(link, "project-card")}">$link</a>"""
  }

  override val itemType: String = HYPERSKILL

  // lexicographical order
  companion object {
    private val SUPPORTED_STEP_TYPES: Set<String> = setOf(
      HyperskillTaskType.CHOICE.type,
      HyperskillTaskType.CODE.type,
      HyperskillTaskType.DATASET.type,
      HyperskillTaskType.MATCHING.type,
      HyperskillTaskType.NUMBER.type,
      HyperskillTaskType.PYCHARM.type,
      HyperskillTaskType.SORTING.type,
      HyperskillTaskType.STRING.type,
      HyperskillTaskType.TABLE.type,
      HyperskillTaskType.TEXT.type,
    )

    fun isStepSupported(type: String?): Boolean = type in SUPPORTED_STEP_TYPES
  }
}
