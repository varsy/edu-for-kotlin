package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createTextChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

class JsCourseProjectGenerator(builder: JsCourseBuilder, course: Course) : CourseProjectGenerator<JsNewProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: JsNewProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    val interpreter = projectSettings.selectedInterpreter
    if (interpreter == null) {
      // It's ok not to have NodeJS interpreter in tests
      if (!isUnitTestMode) {
        LOG.warn("NodeJS interpreter is not selected")
      }
      return
    }
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef())

    val packageJsonFile = project.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    if (packageJsonFile != null && !isUnitTestMode) {
      installNodeDependencies(project, packageJsonFile)
    }

    val modalityState = ModalityState.current()
    interpreter.provideCachedVersionOrFetch { version ->
      invokeLater(modalityState, project.disposed) {
        if (version != null) {
          configureAndAssociateWithProject(project, interpreter, version)
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version")
          @Suppress("UnstableApiUsage")
          val requester = ModuleManager.getInstance(project).modules[0].moduleFile
          NodeSettingsConfigurable.showSettingsDialog(project, requester)
        }
      }
    }
  }

  @Throws(IOException::class)
  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    val packageJsonFile = holder.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    if (packageJsonFile == null && !holder.course.isStudy) {
      val templateText = getInternalTemplateText(NodeModuleNamesUtil.PACKAGE_JSON)
      createTextChildFile(holder, holder.courseDir, NodeModuleNamesUtil.PACKAGE_JSON, templateText)
    }
  }

  companion object {
    private val LOG = logger<JsCourseProjectGenerator>()
  }
}