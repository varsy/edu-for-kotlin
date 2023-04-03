package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE
import com.jetbrains.edu.yaml.EduYamlTaskFilePathReferenceProvider
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem
import java.io.IOException

class TaskFileNotFoundInspection : UnresolvedFileReferenceInspection() {

  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = EduYamlTaskFilePathReferenceProvider.PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(YamlFormatSettings.TASK_CONFIG)

  override fun registerProblem(holder: ProblemsHolder, element: YAMLScalar) {
    val path = element.textValue
    if (isValidFilePath(path)) {
      holder.registerProblem(element, EduYAMLBundle.message("cannot.find.file", path),
                             ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(CreateTaskFileFix(element)).toTypedArray())
    }
    else {
      holder.registerProblem(element, EduYAMLBundle.message("file.invalid.path", path), ProblemHighlightType.ERROR)
    }
  }

  private class CreateTaskFileFix(element: YAMLScalar) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = EduYAMLBundle.message("create.file.quick.fix.family.name")
    override fun getText(): String = EduYAMLBundle.message("create.file.quick.fix.family.name")

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val scalar = startElement as YAMLScalar
      val task = file.virtualFile.getContainingTask(project) ?: return
      val taskDir = task.getDir(project.courseDir) ?: return

      val mapping = scalar.parentOfType<YAMLMapping>() ?: return
      val isVisible = mapping.getKeyValueByKey(VISIBLE)?.valueText != "false"
      val sequenceItem = mapping.parentOfType<YAMLSequenceItem>() ?: return

      val index = sequenceItem.itemIndex
      val path = scalar.textValue
      val taskFile = TaskFile(path, "")
      taskFile.isVisible = isVisible
      // We have to add task file first to keep order
      task.addTaskFile(taskFile, index)
      try {
        GeneratorUtils.createTextChildFile(project, taskDir, path, "")
        val virtualFile = file.originalFile.virtualFile
        if (virtualFile != null) {
          updateEditorNotifications(project, virtualFile)
        }
      } catch (e: IOException) {
        LOG.warn(e)
        // Remove added task file to keep correct task structure
        task.removeTaskFile(path)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(EduYAMLBundle.message("failed.create.file.message", path),
                                   EduYAMLBundle.message("failed.create.file.title"))
        }
      }
    }

    private fun updateEditorNotifications(project: Project, file: VirtualFile) {
      YamlLoader.loadItem(project, file, false)
    }

    companion object {
      private val LOG: Logger = Logger.getInstance(CreateTaskFileFix::class.java)
    }
  }
}
