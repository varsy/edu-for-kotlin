package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.cognifire.PromptActions
import java.util.*

/**
 * Implementation of task which contains task files, tests, input file for tests
 * Update [and StepikChangeRetriever#taskInfoChanged][com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever.taskFilesChanged] if you added new property that has to be compared
 * To implement new task there are 5 steps to be done:
 * - Extend [Task] class
 * - Update [com.jetbrains.edu.learning.stepik.api.StepikJacksonDeserializersKt.doDeserializeTask] to handle json serialization
 * - Update [com.jetbrains.edu.learning.checker.TaskCheckerProvider.getTaskChecker] and provide default checker for new task
 * - Update [com.jetbrains.edu.learning.stepik.StepikTaskBuilder.pluginTaskTypes] for the tasks we do not have separately on stepik
 *   and [com.jetbrains.edu.learning.stepik.StepikTaskBuilder.StepikTaskType] otherwise
 * - Handle yaml deserialization:
 * - add type in [com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeTask]
 * - add yaml mixins for course creator and student fields [com.jetbrains.edu.learning.yaml.format]
 */
abstract class Task : StudyItem {
  private var _taskFiles: MutableMap<String, TaskFile> = LinkedHashMap()
  var taskFiles: Map<String, TaskFile>
    get() = _taskFiles
    set(value) {
      require(value is LinkedHashMap<String, TaskFile>) // taskFiles is supposed to be ordered
      _taskFiles = value
    }

  var feedback: CheckFeedback? = null
  var descriptionText: String = ""
  var descriptionFormat: DescriptionFormat = DescriptionFormat.MD
  var feedbackLink: String? = null

  /**
   * Represents a collection of prompt actions.
   */
  val promptActions = PromptActions()

  /**
   * null means that behaviour for this particular Task hasn't been configured by a user and [Course.solutionsHidden] should be used instead
   */
  var solutionHidden: Boolean? = null
  var record: Int = -1
  var isUpToDate: Boolean = true

  protected var checkStatus: CheckStatus = CheckStatus.Unchecked

  open var status: CheckStatus
    get() = checkStatus
    set(status) {
      for (taskFile in _taskFiles.values) {
        for (placeholder in taskFile.answerPlaceholders) {
          placeholder.status = status
        }
      }
      if (checkStatus !== status) {
        feedback = null
      }
      checkStatus = status
    }

  val lesson: Lesson
    get() = parent as? Lesson ?: error("Lesson is null for task $name")

  open val isPluginTaskType: Boolean
    get() = true
  open val supportSubmissions: Boolean
    get() = false
  open val isToSubmitToRemote: Boolean
    get() = false
  open val isChangedOnFailed: Boolean // For retry button: true means task description changes after failing
    get() = false

  override val course: Course
    get() = lesson.course

  constructor() // used for deserialization
  constructor(name: String) : super(name)
  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name) {
    this.id = id
    this.index = position
    this.updateDate = updateDate
    checkStatus = status
  }

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    parent = parentItem
    for (taskFile in _taskFiles.values) {
      taskFile.initTaskFile(this, isRestarted)
    }
  }

  fun getTaskFile(name: String): TaskFile? {
    return _taskFiles[name]
  }

  fun addTaskFile(name: String, isVisible: Boolean = true): TaskFile {
    val taskFile = TaskFile()
    taskFile.task = this
    taskFile.name = name
    taskFile.isVisible = isVisible
    _taskFiles[name] = taskFile
    return taskFile
  }

  fun addTaskFile(taskFile: TaskFile) {
    taskFile.task = this
    _taskFiles[taskFile.name] = taskFile
  }

  fun addTaskFile(taskFile: TaskFile, position: Int) {
    taskFile.task = this
    if (position < 0 || position > _taskFiles.size) {
      throw IndexOutOfBoundsException()
    }
    val newTaskFileMap = LinkedHashMap<String, TaskFile>(_taskFiles.size + 1)
    var currentIndex = 0
    for ((key, value) in _taskFiles) {
      if (currentIndex == position) {
        newTaskFileMap[taskFile.name] = taskFile
      }
      newTaskFileMap[key] = value
      currentIndex++
    }
    if (currentIndex == position) {
      newTaskFileMap[taskFile.name] = taskFile
    }
    _taskFiles = newTaskFileMap
  }

  //used for yaml deserialization
  private fun setTaskFileValues(taskFiles: List<TaskFile>) {
    _taskFiles.clear()
    for (taskFile in taskFiles) {
      _taskFiles[taskFile.name] = taskFile
    }
  }

  fun getTaskFileValues(): Collection<TaskFile> {
    return _taskFiles.values
  }

  fun removeTaskFile(taskFile: String): TaskFile? {
    return _taskFiles.remove(taskFile)
  }

  fun taskFileIndex(taskFile: String): Int? {
    val index = _taskFiles.keys.toList().indexOf(taskFile)
    return if (index == -1) null else index
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val task = other as Task
    if (index != task.index) return false
    if (name != task.name) return false
    if (_taskFiles != task._taskFiles) return false
    if (descriptionText != task.descriptionText) return false
    return descriptionFormat == task.descriptionFormat
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + index
    result = 31 * result + _taskFiles.hashCode()
    result = 31 * result + descriptionText.hashCode()
    result = 31 * result + descriptionFormat.hashCode()
    return result
  }

  companion object {
    @JvmStatic
    protected val LOG = logger<Task>()
  }
}
