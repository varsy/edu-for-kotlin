package com.jetbrains.edu.ai.translation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.educational.translation.enum.Language
import com.jetbrains.educational.translation.format.CourseTranslation
import com.jetbrains.educational.translation.format.DescriptionText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class TranslationLoader(private val project: Project, private val scope: CoroutineScope) {
  private val lock = AtomicBoolean(false)

  private val isLocked: Boolean
    get() = lock.get()

  private fun lock(): Boolean {
    return lock.compareAndSet(false, true)
  }

  private fun unlock() {
    lock.set(false)
  }

  fun fetchAndApplyTranslation(course: EduCourse, language: Language) {
    scope.launch {
      try {
        if (!lock()) {
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIBundle.message("ai.translation.already.running")
          )
          return@launch
        }
        withBackgroundProgress(project, EduAIBundle.message("ai.service.getting.course.translation")) {
          if (!course.isTranslationExists(language)) {
            val translation = loadAndSaveAsync(course, language)
            course.saveTranslation(translation)
          }
          course.translatedToLanguageCode = language.code
          withContext(Dispatchers.EDT) {
            YamlFormatSynchronizer.saveItem(course)
            TaskToolWindowView.getInstance(project).updateTaskDescription()
          }
        }
      }
      finally {
        unlock()
      }
    }
  }

  private suspend fun loadAndSaveAsync(course: EduCourse, language: Language): CourseTranslation =
    withContext(Dispatchers.IO) {
      downloadTranslation(course, language).onError { error ->
        error("Failed to download translation for ${course.name} to $language: $error")
      }
    }

  private suspend fun EduCourse.isTranslationExists(language: Language): Boolean =
    readAction {
      allTasks.all {
        it.getDescriptionFile(project, translatedToLanguageCode = language.code)?.exists() == true
      }
    }

  private suspend fun downloadTranslation(course: EduCourse, language: Language): Result<CourseTranslation, String> {
    val marketplaceId = course.marketplaceId
    val updateVersion = course.updateVersion

    repeat(DOWNLOAD_ATTEMPTS) {
      val translation = TranslationServiceConnector.getInstance().getTranslatedCourse(marketplaceId, updateVersion, language)
        .onError { error -> return Err(error) }
      if (translation != null) {
        return Ok(translation)
      }
      LOG.debug("Translation hasn't been downloaded yet, trying again in $DOWNLOAD_TIMEOUT_SECONDS seconds")
      delay(DOWNLOAD_TIMEOUT_SECONDS.seconds)
    }
    return Err("Translation wasn't downloaded after $DOWNLOAD_ATTEMPTS attempts")
  }

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslation) {
    val taskDescriptions = courseTranslation.taskDescriptions
    writeAction {
      for (task in allTasks) {
        val translation = taskDescriptions[task.taskEduId] ?: continue
        task.saveTranslation(translation)
      }
    }
  }

  @RequiresBlockingContext
  private fun Task.saveTranslation(text: DescriptionText) {
    val taskDirectory = getTaskDirectory(project) ?: return
    val name = descriptionFormat.fileNameWithTranslation(text.language.code)

    try {
      GeneratorUtils.createTextChildFile(project, taskDirectory, name, text.text)
    }
    catch (exception: IOException) {
      LOG.error("Failed to write text to $taskDirectory", exception)
      throw exception
    }
  }

  companion object {
    private val LOG = thisLogger()
    private const val DOWNLOAD_ATTEMPTS: Int = 20
    private const val DOWNLOAD_TIMEOUT_SECONDS: Int = 3

    fun getInstance(project: Project): TranslationLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).isLocked
  }
}