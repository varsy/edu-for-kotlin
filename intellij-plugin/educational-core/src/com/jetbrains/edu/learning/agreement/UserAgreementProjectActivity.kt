package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.isHeadlessEnvironment

class UserAgreementProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject()) {
      return
    }
    if (!userAgreementSettings().isNotShown && !isHeadlessEnvironment) {
      UserAgreementManager.getInstance().showUserAgreement(project)
    }
  }
}