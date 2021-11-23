package com.jetbrains.edu.learning.codeforces.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.*
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider.Companion.getLanguageIdAndVersion
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider.Companion.joinCourse
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import org.jetbrains.annotations.NonNls

class StartCodeforcesContestAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val showViewAllLabel = e.place != CodeforcesCoursesPanel.PLACE
    val contestId = showDialogAndGetContestId(showViewAllLabel) ?: return
    joinContests(contestId, null)
  }

  private fun showDialogAndGetContestId(showViewAllLabel: Boolean): Int? {
    val dialog = ImportCodeforcesContestDialog(showViewAllLabel)
    if (!dialog.showAndGet()) {
      return null
    }
    return dialog.getContestId()
  }

  companion object {

    @NonNls
    const val ACTION_ID = "Educational.Codeforces.StartContest"

    @VisibleForTesting
    fun getContestUnderProgress(contestParameters: ContestParameters): Result<CodeforcesCourse, String> =
      ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<CodeforcesCourse, String>, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          EduUtils.execCancelable {
            CodeforcesConnector.getInstance().getContest(contestParameters)
          }
        }, EduCoreBundle.message("codeforces.getting.contest.information"), true, null
      )


    private fun showFailedToGetContestInfoNotification(contestId: Int, error: String) {
      val contestUrl = CodeforcesContestConnector.getContestURLFromID(contestId)
      Messages.showErrorDialog(
        EduCoreBundle.message(
          "codeforces.error.failed.to.get.contest.information",
          CodeforcesNames.CODEFORCES_TITLE,
          error.toLowerCase(),
          contestUrl
        ),
        EduCoreBundle.message("codeforces.error.failed.to.load.contest.title", CodeforcesNames.CODEFORCES_TITLE)
      )
    }

    fun joinContests(contestId: Int, coursePanel: CoursePanel?) {
      val codeforcesCourse = getContestInfoUnderProgress(contestId).onError {
        showFailedToGetContestInfoNotification(contestId, it)
        error("Failed to get contest info for contest with id=$contestId")
      }
      val contestName = codeforcesCourse.name
      val contestLanguages = codeforcesCourse.availableLanguages

      if (contestLanguages.isEmpty()) {
        showNoSupportedLanguagesForContestNotification(contestName)
        error("Cannot load available languages: $contestId")
      }

      val dialog = ChooseCodeforcesContestLanguagesDialog(codeforcesCourse)
      if (!dialog.showAndGet()) {
        return
      }

      val taskTextLanguage = dialog.selectedTaskTextLanguage()
      val language = dialog.selectedLanguage()
      val languageIdAndVersion = getLanguageIdAndVersion(language) ?: return

      val contestParameters = ContestParameters(
        codeforcesCourse.id,
        languageIdAndVersion,
        taskTextLanguage.locale,
        codeforcesCourse.endDateTime,
        language
      )

      when (val contestResult = getContestUnderProgress(contestParameters)) {
        is Err -> {
          showFailedToGetContestInfoNotification(codeforcesCourse.id, contestResult.error)
          error("Error whe getting contest with id=$contestId: ${contestResult.error}")
        }
        is Ok -> {
          val contest = contestResult.value
          val courseInfo = CourseInfo(contest, { dialog.contestLocation() }, { dialog.languageSettings() })
          joinCourse(courseInfo, CourseMode.STUDY, coursePanel) {}
        }
      }
    }

    private fun getContestInfoUnderProgress(contestId: Int): Result<CodeforcesCourse, String> =
      ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<CodeforcesCourse, String>, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          EduUtils.execCancelable {
            CodeforcesConnector.getInstance().getContestInformation(contestId)
          }
        }, EduCoreBundle.message("codeforces.getting.available.languages"), true, null
      )

    private fun showNoSupportedLanguagesForContestNotification(contestName: String) {
      Messages.showErrorDialog(
        EduCoreBundle.message("codeforces.error.no.supported.languages", contestName),
        EduCoreBundle.message("codeforces.error.failed.to.load.contest.title", CodeforcesNames.CODEFORCES_TITLE)
      )
    }
  }
}