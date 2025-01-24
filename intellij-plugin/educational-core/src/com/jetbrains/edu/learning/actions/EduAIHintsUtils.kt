package com.jetbrains.edu.learning.actions

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import org.jetbrains.annotations.NonNls

/**
 * @see [com.jetbrains.edu.aiHints.core]
 */
object EduAIHintsUtils {
  /**
   * @see [com.jetbrains.edu.aiHints.core.action.GetHint]
   */
  @NonNls
  const val GET_HINT_ACTION_ID: String = "Educational.Hints.GetHint"

  /**
   * Temporary solution to check if the action is available for current task.
   * Will be removed as soon as proper checking of the existence of the corresponding function has been implemented.
   * Such check should be done via verifying the existence of the required EPs.
   *
   * @see [com.jetbrains.edu.aiHints.core.action.GetHint]
   */
  fun isGetHintAvailable(task: Task): Boolean {
    if (!isFeatureEnabled(EduExperimentalFeatures.AI_HINTS) || !UserAgreementSettings.getInstance().aiServiceAgreement) return false
    val course = task.course as? EduCourse ?: return false
    val isMarketplaceCourse = course.isStudy && course.isMarketplaceRemote
    val isRequiredLanguage = course.languageId == EduFormatNames.KOTLIN || course.languageId == EduFormatNames.PYTHON
    return isRequiredLanguage && isMarketplaceCourse && task is EduTask && task.status == CheckStatus.Failed
  }

  /**
   * Temporary solution because currently [com.jetbrains.edu.aiHints.core.action.GetHint]'s presentation is not propagated to corresponding
   * button in the [com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckDetailsPanel].
   *
   * (see [EDU-7584](https://youtrack.jetbrains.com/issue/EDU-7584))
   */
  @Service(Service.Level.PROJECT)
  class HintStateManager {
    @Volatile
    private var state: HintState = HintState.DEFAULT

    private enum class HintState {
      DEFAULT, ACCEPTED;
    }

    fun reset() {
      state = HintState.DEFAULT
    }

    fun acceptHint() {
      state = HintState.ACCEPTED
    }

    companion object {
      fun isDefault(project: Project): Boolean = getInstance(project).state == HintState.DEFAULT

      fun getInstance(project: Project): HintStateManager = project.service()
    }
  }
}