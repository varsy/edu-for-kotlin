package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service
class UserAgreementManager(scope: CoroutineScope) {
  init {
    scope.launch {
      userAgreementSettings().userAgreementProperties.collect {
        reloadProjectOnAgreementChange()
      }
    }
  }

  fun showUserAgreement(project: Project) {
    runInEdt {
      UserAgreementDialog.showUserAgreementDialog(project)
    }
  }

  private fun reloadProjectOnAgreementChange() {
    val projectManager = ProjectManager.getInstance()
    for (openProject in projectManager.openProjects) {
      if (openProject.isEduYamlProject() && !openProject.isDisposed) {
        projectManager.reloadProject(openProject)
      }
    }
  }

  companion object {
    fun getInstance(): UserAgreementManager = service()
  }
}