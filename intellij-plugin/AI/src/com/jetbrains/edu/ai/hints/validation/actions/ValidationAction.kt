package com.jetbrains.edu.ai.hints.validation.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.hints.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.ai.hints.validation.util.StudentSolutionRecord
import com.jetbrains.edu.ai.hints.validation.util.parseCsvFile
import com.jetbrains.edu.ai.hints.validation.util.propagateAuthorSolution
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.ext.updateContent
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEach
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/**
 * Abstract class for performing validation actions in the educational AI assistant.
 * It provides a framework for subclasses to define and implement specific validation tasks, such as automatic validation or
 * preparing datasets for manual validation.
 *
 * @param T the type of records generated during validation
 *
 * @property outputFilePrefixName the prefix name of the file where the validation results will be saved
 * @property name the name of the validation action
 *
 * @see ActionWithProgressIcon
 * @see DumbAware
 */
abstract class ValidationAction<T> : ActionWithProgressIcon(), DumbAware {
  protected abstract val outputFilePrefixName: String
  protected abstract val name: String
  protected abstract val isNavigationRequired: Boolean
  private val validationOutputPath by lazy {
    Path(System.getProperty("validation.output.path", "validationOutput")).also {
      it.createDirectories()
    }
  }
  private val outputFileName: String by lazy { "${outputFilePrefixName}_${LocalDateTime.now()}.csv" }
  private val validationOutputFile: File by lazy { (validationOutputPath / outputFileName).toFile() }
  private val pathToSolutions by lazy { Path(System.getProperty("validation.solution.path")) }
  protected abstract val pathToLabelledDataset: Path?

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  protected abstract suspend fun buildRecords(task: EduTask, lesson: Lesson): List<T>
  protected open suspend fun buildRecords(manualValidationRecord: T): T = throw UnsupportedOperationException("This function is not supported.")

  protected abstract fun MutableList<T>.convertToDataFrame(): DataFrame<T>

  protected abstract fun CSVRecord.toDataframeRecord(): T

  protected open val accuracyCalculator: AccuracyCalculator<T>? = null

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return
    ProgressManager.getInstance().run(ValidationTask(project, course))
  }

  inner class ValidationTask(
    project: Project,
    private val course: Course
  ) : Backgroundable(project, name) {
    override fun run(indicator: ProgressIndicator) {
      processStarted()

      indicator.isIndeterminate = false
      indicator.fraction = 0.0
      val totalTasks = course.allTasks.size
      var doneTasks = 0
      var previousTask: Task? = null

      val studentSolutions = parseCsvFile(pathToSolutions) { record ->
        StudentSolutionRecord.buildFrom(record)
      }
      val manualValidationDataset = parseCsvFile(pathToLabelledDataset) { it.toDataframeRecord() }

      val records = mutableListOf<T>()
      if (manualValidationDataset != null && accuracyCalculator != null) {
        manualValidationDataset.forEach { manualValidationRecord ->
          indicator.text = EduAIBundle.message("validation.validating", doneTasks)
          indicator.fraction = doneTasks.toDouble() / totalTasks

          runBlockingCancellable {
            records.add(buildRecords(manualValidationRecord))
            records.writeCsvIfNeeded()
          }
          doneTasks++
        }
        accuracyCalculator?.let {
          records.add(it.calculateValidationAccuracy(manualValidationDataset, records))
        }
        records.convertToDataFrame().writeCSV()
      } else {
        for (lesson in course.lessons) {
          val lessonRecords = mutableListOf<T>()
          val firstTask = lesson.taskList[0]
          ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
              NavigationUtils.navigateToTask(project, firstTask)
            }
          }
          val currentTask = TaskToolWindowView.getInstance(project).currentTask
          for (task in lesson.taskList) {
            if (task is EduTask) {
              if (isNavigationRequired) {
                ApplicationManager.getApplication().invokeAndWait {
                  ApplicationManager.getApplication().runWriteAction {
                    NavigationUtils.navigateToTask(project, task, fromTask = currentTask, showDialogIfConflict = false)
                  }
                }
              }

              indicator.text = EduAIBundle.message("validation.validating", task.name)
              indicator.fraction = doneTasks.toDouble() / totalTasks

              studentSolutions?.let {
                it.getSolutionListForTask(lesson.name, task.name).forEach { studentCode ->
                  ApplicationManager.getApplication().invokeAndWait {
                    lesson.replaceContent(task, studentCode, project)
                  }
                  runBlockingCancellable {
                    lessonRecords.addAll(buildRecords(task, lesson))
                  }
                }
              } ?: run {
                previousTask?.let {
                  propagateAuthorSolution(it, task, project)
                }
                runBlockingCancellable {
                  lessonRecords.addAll(buildRecords(task, lesson))
                }
              }

              doneTasks++
            }
            previousTask = task
          }
          lessonRecords.convertToDataFrame().writeCSV()
          records.addAll(lessonRecords)
        }
        accuracyCalculator?.let {
          mutableListOf(it.calculateOverallAccuracy(records)).convertToDataFrame().writeCSV()
        }
      }

      indicator.fraction = 1.0
      indicator.text = EduAIBundle.message("validation.writing.results")

      processFinished()
    }

    private fun List<StudentSolutionRecord>.getSolutionListForTask(lessonName: String, taskName: String) =
      filter { it.lessonName == lessonName && it.taskName == taskName }.map { it.code }

    private fun DataFrame<T>.writeCSV() {
      val format = CSVFormat.DEFAULT
      val shouldWriteHeaders = !validationOutputFile.exists()
      val writer = FileWriter(validationOutputFile, true)
      format.print(writer).use { printer ->
        if (shouldWriteHeaders) {
          printer.printRecord(columnNames())
        }
        forEach {
          printer.printRecord(it.values())
        }
      }
    }

    private fun MutableList<T>.writeCsvIfNeeded() {
      if (size > MAX_RECORDS_IN_MEMORY) {
        convertToDataFrame().writeCSV()
        clear()
      }
    }

    override fun onThrowable(error: Throwable) {
      NotificationGroupManager.getInstance().getNotificationGroup("AiEduAssistantValidation")
        .createNotification(error.message ?: "Error during validation", NotificationType.ERROR)
        .notify(project)
      super.onThrowable(error)
    }

    override fun onFinished() {
      NotificationGroupManager.getInstance().getNotificationGroup("AiEduAssistantValidation")
        .createNotification(EduAIBundle.message(
          "validation.finished",
          name,
          validationOutputFile
        ), NotificationType.INFORMATION)
        .notify(project)
      super.onFinished()
    }
  }

  private fun changeStateForMainFile(task: Task, newCode: String, mainFileName: String = MAIN_FILE_NAME) {
    val project = task.project ?: error("Cannot get project")
    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    val externalState = task.taskFiles.mapValues {
      if (mainFileName in it.key) {
        newCode
      }
      else {
        it.value.contents.textualRepresentation
      }
    }
    frameworkLessonManager.saveExternalChanges(task, externalState)
  }

  protected fun Lesson.replaceContent(task: Task, newCode: String, project: Project) {
    val taskFile = project.selectedTaskFile ?: error("Can't get task file")
    taskFile.updateContent(project, newCode)
    if (this is FrameworkLesson) {
      changeStateForMainFile(task, newCode)
    }
  }

  companion object {
    private const val MAIN_FILE_NAME = "Main.kt"
    private const val MAX_RECORDS_IN_MEMORY = 50
  }
}
