package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.studyItemType
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import javax.swing.SwingConstants


class CodeforcesCreateTestAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {

    val project = e.project ?: return
    if (project.isDisposed) return

    val selectedTask = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)?.getContainingTask(project) as? CodeforcesTask ?: return
    val taskDir = selectedTask.getDir(project.courseDir) ?: return
    val defaultTestName = getDefaultTestName(selectedTask, project)
    val testDataDir = runWriteAction { VfsUtil.createDirectoryIfMissing(taskDir, CodeforcesNames.TEST_DATA_FOLDER) }
    val validator = CCStudyItemPathInputValidator(project, selectedTask.course, selectedTask.studyItemType, testDataDir)
    val contentPanel = NewItemSimplePopupPanel()

    val nameField = contentPanel.textField
    nameField.text = defaultTestName
    val popup = NewItemPopupUtil.createNewItemPopup(EduCoreBundle.message("dialog.title.test.name"), contentPanel, nameField)

    contentPanel.setApplyAction {
      val testName = nameField.text
      if (validator.checkInput(testName)) {
        popup.closeOk(it)
        val inputFile = GeneratorUtils.createTextChildFile(project, testDataDir, GeneratorUtils.joinPaths(testName, selectedTask.inputFileName),
                                                       "") ?: return@setApplyAction
        val outputFile = GeneratorUtils.createTextChildFile(project, testDataDir,
                                                        GeneratorUtils.joinPaths(testName, selectedTask.outputFileName), "")
                         ?: return@setApplyAction
        openInSplitEditors(project, outputFile, inputFile)
      }
      else {
        val errorMessage = (validator as InputValidatorEx).getErrorText(testName)
        contentPanel.setError(errorMessage)
      }
    }

    popup.showCenteredInCurrentWindow(project)
  }

  private fun openInSplitEditors(project: Project, outputFile: VirtualFile, inputFile: VirtualFile) {
    val fileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)

    if (fileEditorManagerEx.isInSplitter) {
      val windows = fileEditorManagerEx.splitters.getWindows()
      fileEditorManagerEx.openFiles(outputFile, inputFile, windows)
    }
    else {
      fileEditorManagerEx.openFile(inputFile, true, false)
      fileEditorManagerEx.currentWindow?.split(SwingConstants.HORIZONTAL, true, outputFile, false)
    }
  }

  private fun getDefaultTestName(task: CodeforcesTask, project: Project): String {
    val maxOrNull = task.getTestFolders(project).mapNotNull { it.name.toIntOrNull() }.maxOrNull()
    return if (maxOrNull == null) "1" else (maxOrNull + 1).toString()
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return

    if (CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)?.getContainingTask(project) !is CodeforcesTask) return

    presentation.isEnabledAndVisible = true
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Codeforces.CreateTestSample"
  }
}
