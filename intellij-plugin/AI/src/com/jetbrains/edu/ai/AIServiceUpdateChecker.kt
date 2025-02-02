package com.jetbrains.edu.ai

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.ai.terms.updater.TermsUpdateChecker
import com.jetbrains.edu.ai.translation.updater.TranslationUpdateChecker
import com.jetbrains.edu.learning.courseFormat.EduCourse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class AIServiceUpdateChecker(private val project: Project, private val scope: CoroutineScope) {
  private val lock = AtomicBoolean(false)

  private val isRunning: Boolean
    get() = lock.get()

  private val checkInterval: Duration
    get() {
      // Default value is 3600 seconds (1 hour), set in AI.xml
      return Registry.intValue(REGISTRY_KEY).seconds
    }

  private fun launch(course: EduCourse) {
    if (lock.compareAndSet(false, true)) {
      scope.launch {
        while (true) {
          TranslationUpdateChecker.getInstance(project).checkUpdate(course)
          TermsUpdateChecker.getInstance(project).checkUpdate(course)
          delay(checkInterval)
        }
      }
    }
    else {
      LOG.error("AI service update checker is already running")
    }
  }

  companion object {
    private val LOG = Logger.getInstance(AIServiceUpdateChecker::class.java)

    private const val REGISTRY_KEY: String = "edu.ai.translation.update.check.interval"

    fun Project.launchUpdateChecker(course: EduCourse) {
      val checker = service<AIServiceUpdateChecker>()
      if (!checker.isRunning) {
        checker.launch(course)
      }
    }
  }
}