package com.jetbrains.edu.learning.hints

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.hints.context.FunctionSignature
import com.jetbrains.edu.learning.hints.context.SignatureSource
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.educational.ml.hints.context.TestFailureContext
import com.jetbrains.educational.ml.hints.processors.TaskProcessor
import com.jetbrains.rd.util.firstOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class TaskProcessorImpl(val task: Task) : TaskProcessor {
  var currentTaskFile: TaskFile? = null

  private val project = task.project ?: error("Project was not found")

  private val language = task.course.languageById ?: error("Language was not found")

  override fun getFullTaskFileText() = runReadAction {
    project.selectedTaskFile?.getText(project) ?: error("Failed to retrieve the text of the task file")
  }

  override fun getLowercaseLanguageDisplayName() = language.displayName.lowercase()

  override fun getSubmissionTextRepresentation() = runReadAction { getChangedContent(task, project) }

  private fun getTaskContentHtmlDocument(): Document {
    val taskText = task.getTaskText(project) ?: task.descriptionText
    return Jsoup.parse(taskText.trimIndent())
  }

  override fun getTaskTextRepresentation(): String {
    val document = getTaskContentHtmlDocument()
    document.getElementsByClass(HTML_HINT_CLASS_NAME).remove()
    return document.text()
  }

  override fun getTestFailureContext(): TestFailureContext? {
    val feedback = task.feedback ?: return null
    val name = feedback.failedTestInfo?.name ?: return null
    if (task.status != CheckStatus.Failed) return null
    return TestFailureContext(
      name = name,
      message = feedback.message,
      expected = feedback.expected,
      actual = feedback.actual,
      details = feedback.failedTestInfo?.details,
      isCompilationError = feedback.failedTestInfo?.message == COMPILATION_FAILED_MESSAGE
    )
  }

  override fun getHintsTextRepresentation(): List<String> {
    val document = getTaskContentHtmlDocument()
    return document.getElementsByClass(HTML_HINT_CLASS_NAME).map { it.text() }
  }

  override fun getTheoryTextRepresentation(): String {
    val tasks = task.lesson.taskList
    return tasks.subList(0, tasks.indexOf(task)).filterIsInstance<TheoryTask>().joinToString(System.lineSeparator()) { it.presentableName }
  }

  override fun getFunctionsFromTask(): List<String>? {
    val project = task.project ?: return null
    return task.taskFiles.values.filterNot { it.isTestFile }.flatMap { file ->
      getFunctionSignaturesIfFileUnchanged(file, project)?.let {
        return@flatMap it
      } ?: run {
        runReadAction { getFunctionSignatures(task, file, project) }.also {
          file.functionSignatures = it
        }
      }
    }.map { it.toString() }
  }

  private fun getFunctionSignaturesIfFileUnchanged(file: TaskFile, project: Project) =
    if (isFileUnchanged(file, project) && file.functionSignaturesSnapshotHash == file.snapshotFileHash)
      file.functionSignatures
    else null
      .also { file.functionSignaturesSnapshotHash = file.snapshotFileHash }

  private fun isFileUnchanged(file: TaskFile, project: Project): Boolean {
    val document = file.getDocument(project) ?: return false
    val currentHash = document.text.hashCode()
    val isUnchanged = file.snapshotFileHash == currentHash
    if (!isUnchanged) file.snapshotFileHash = currentHash
    return isUnchanged
  }

  private fun getFunctionSignatures(task: Task, file: TaskFile, project: Project): List<FunctionSignature> {
    val language = task.course.languageById ?: return emptyList()
    val virtualFile = file.getVirtualFile(project) ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()
    val functionSignatures = FunctionSignaturesProvider.getFunctionSignatures(
      psiFile, if (file.isVisible) SignatureSource.VISIBLE_FILE else SignatureSource.HIDDEN_FILE, language
    )
    return functionSignatures
  }


  override fun getFunctionsSetStrFromAuthorSolution() = task.authorSolutionContext?.functionSignatures?.map { it.toString() }

  override fun getStringsFromAuthorSolution() = task.authorSolutionContext?.functionsToStringMap?.values?.flatten()

  private fun getStringsIfFileUnchanged(file: TaskFile, project: Project): List<String>? = if (isFileUnchanged(
      file,
      project
    ) && file.usedStringsSnapshotHash == file.snapshotFileHash) file.usedStrings
  else null.also { file.usedStringsSnapshotHash = file.snapshotFileHash }

  override fun getStringsFromTask(): List<String> {
    val project = task.project ?: return emptyList()
    val language = task.course.languageById ?: return emptyList()
    return task.taskFiles.values.filterNot { it.isTestFile }.flatMap { file ->
      getStringsIfFileUnchanged(file, project)?.let {
        return@flatMap it
      } ?: run {
        val virtualFile = file.getVirtualFile(project) ?: return@flatMap emptyList()
        runReadAction {
          val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@runReadAction emptyList()
          StringExtractor.getFunctionsToStringsMap(psiFile, language).value.values.flatten()
        }.also {
          file.usedStrings = it
        }
      }
    }
  }

  override fun getShortFunctionFromSolutionIfRecommended(code: String, functionName: String): String? {
    val functionSignatures = getFunctionSignaturesFromGeneratedCode(code, project, language)
    val signature = functionSignatures.find { it.name == functionName }
    val functionsSignaturesFromSolution = task.authorSolutionContext?.functionSignatures?.filter {
      it.bodyLineCount != null && (it.bodyLineCount ?: Int.MAX_VALUE) <= MAX_BODY_LINES_IN_SHORT_FUNCTION
    } ?: return null
    if (signature != null && functionsSignaturesFromSolution.contains(signature)) {
      val taskFile = currentTaskFile ?: project.selectedTaskFile ?: error("Can't get task file")
      return runReadAction {
        val psiFileSolution = taskFile.getSolution().createPsiFileForSolution(project, language)
        FunctionSignatureResolver.getFunctionBySignature(
          psiFileSolution, signature.name, language
        )?.text ?: error("Cannot get the function from the author's solution")
      }
    }
    return null
  }

  private fun getFunctionSignaturesFromGeneratedCode(code: String, project: Project, language: Language) = runReadAction {
    val psiFile = code.createPsiFileForSolution(project, language)
    FunctionSignaturesProvider.getFunctionSignatures(psiFile, SignatureSource.GENERATED_SOLUTION, language)
  }

  override fun extractRequiredFunctionsFromCodeHint(codeHint: String): String {
    val codeHintPsiFile = runReadAction {
      PsiFileFactory.getInstance(project).createFileFromText(CODE_HINT_PSI_FILE_NAME, language, codeHint)
    }
    return codeHintPsiFile.filterAllowedModifications(task, project, SignatureSource.GENERATED_SOLUTION)
  }

  override fun getModifiedFunctionNameInCodeHint(codeStr: String, codeHint: String) = runReadAction {
    val codeHintPsiFile = PsiFileFactory.getInstance(project).createFileFromText(CODE_HINT_PSI_FILE_NAME, language, codeHint)
    val codePsiFile = PsiFileFactory.getInstance(project).createFileFromText(CODE_PSI_FILE_NAME, language, codeStr)
    val functionName = FilesDiffer.findDifferentMethods(codePsiFile, codeHintPsiFile, language, true)?.firstOrNull()
    ?: error("The code prompt didn't make any difference")
    val selectedTaskFile = project.selectedTaskFile
    val taskName = task.taskFilesWithChangedFunctions
      ?.filter { (_, functions) -> functionName in functions }
      ?.firstOrNull()
      ?.key ?: selectedTaskFile ?: error("Can't get task name")
    currentTaskFile = task.taskFiles[taskName] ?: selectedTaskFile ?: error("Can't get task file")
    functionName
  }

  private fun getFunctionPsiWithName(code: String, functionName: String, project: Project, language: Language) = runReadAction {
    val codePsiFile = PsiFileFactory.getInstance(project).createFileFromText(CODE_PSI_FILE_NAME, language, code)
    FunctionSignatureResolver.getFunctionBySignature(codePsiFile, functionName, language)
  }

  override fun reduceChangesInCodeHint(code: String, modifiedCode: String, functionName: String): String {
    val functionFromCode = getFunctionPsiWithName(code, functionName, project, language)?.copy()
    val functionFromCodeHint = getFunctionPsiWithName(modifiedCode, functionName, project, language)?.copy()
                               ?: error("Function with the name $functionName in the code hint is not found")
    val reducedCodeHint = FunctionDiffReducer.reduceDiffFunctions(functionFromCode, functionFromCodeHint, project, language)
    return runReadAction { reducedCodeHint?.text } ?: modifiedCode
  }

  override fun applyCodeHint(codeHint: String): String? {
    val taskFile = currentTaskFile ?: project.selectedTaskFile ?: return null
    val virtualFile = taskFile.getVirtualFile(project) ?: return null
    val virtualFileText = runReadAction { virtualFile.getTextFromTaskTextFile() } ?: return null
    val psiFileCopy = runReadAction {
      PsiFileFactory.getInstance(project).createFileFromText(
        "copy${virtualFile.name}", language, virtualFileText
      )
    }
    var isFileModified = false
    val codeHintPsiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText(CODE_HINT_PSI_FILE_NAME, language, codeHint) }
    val functionSignaturesFromCodeHint = runReadAction {
      FunctionSignaturesProvider.getFunctionSignatures(
        codeHintPsiFile, SignatureSource.GENERATED_SOLUTION, language
      )
    }

    for (newFunction in functionSignaturesFromCodeHint) {
      runReadAction { FunctionSignatureResolver.getFunctionBySignature(codeHintPsiFile, newFunction.name, language) }?.let { psiNewFunction ->
        WriteCommandAction.runWriteCommandAction(project, null, null, {
          FunctionSignatureResolver.getFunctionBySignature(psiFileCopy, newFunction.name, language)?.replace(psiNewFunction)?.let {
            isFileModified = true
          } ?: run {
            psiFileCopy.add(psiNewFunction)
            isFileModified = true
          }
        }, psiFileCopy)
      }
    }
    if (!isFileModified) return null
    return psiFileCopy.text
  }

  override fun applyInspections(code: String) = applyInspections(code, project, language)

  override fun containsGeneratedCodeStructures(code: String) = getFunctionSignaturesFromGeneratedCode(code, project, language).isNotEmpty()

  private fun getChangedContent(task: Task, project: Project): String? {
    findChangedFunctions(task, project)
    return task.taskFilesWithChangedFunctions?.keys?.joinToString(separator = System.lineSeparator()) { taskFileName ->
      val virtualFile = task.taskFiles[taskFileName]?.getVirtualFile(project) ?: return@joinToString ""
      val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@joinToString ""
      psiFile.filterAllowedModifications(task, project, SignatureSource.VISIBLE_FILE)
    }
  }

  private fun findChangedFunctions(task: Task, project: Project) {
    if (task.taskFilesWithChangedFunctions != null) return
    val language = task.course.languageById ?: return
    val previousTask = NavigationUtils.previousTask(task) ?: return
    val taskFileNamesToChangedFunctions = mutableMapOf<String, List<String>>()
    val visibleTaskFiles = task.taskFiles.values.filter { it.isVisible }
    for (taskFile in visibleTaskFiles) {
      val previousTaskFile = previousTask.taskFiles[taskFile.name] ?: continue
      val beforePsiFile = previousTaskFile.getSolution().createPsiFileForSolution(project, language)
      val afterPsiFile = taskFile.getSolution().createPsiFileForSolution(project, language)
      val changedFunctions = FilesDiffer.findDifferentMethods(beforePsiFile, afterPsiFile, language)
      if (!changedFunctions.isNullOrEmpty()) {
        taskFileNamesToChangedFunctions[taskFile.name] = changedFunctions
      }
    }
    if (taskFileNamesToChangedFunctions.isNotEmpty()) {
      task.taskFilesWithChangedFunctions = taskFileNamesToChangedFunctions
    }
  }

  /**
   * Filters the allowed changed functions in the psi file at the current stage:
   * which are either contained in task.changedFunctions (updated functions in the authoring solution in comparison with the previous step)
   * or not contained in the author's solution.
   */
  private fun PsiFile.filterAllowedModifications(task: Task, project: Project, signatureSource: SignatureSource): String {
    findChangedFunctions(task, project)
    val language = task.course.languageById ?: return ""
    return runReadAction {
      FunctionSignaturesProvider.getFunctionSignatures(this, signatureSource, language).filter { functionSignature ->
        task.taskFilesWithChangedFunctions?.values?.flatten()?.contains(functionSignature.name) == true ||
        task.authorSolutionContext?.functionSignatures?.contains(functionSignature) == false
      }.joinToString(separator = System.lineSeparator()) {
        FunctionSignatureResolver.getFunctionBySignature(this, it.name, language)?.text ?: ""
      }
    }
  }

  companion object {
    fun applyInspections(code: String, project: Project, language: Language): String {
      val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("file", language, code) }
      val inspections = runReadAction { InspectionProvider.getInspections(language) }
      for (inspection in inspections) {
        psiFile.applyLocalInspection(inspection).forEach { descriptor ->
          descriptor.fixes?.firstOrNull()?.let { quickFix ->
            WriteCommandAction.runWriteCommandAction(project, null, null, {
              quickFix.applyFix(project, descriptor) }, psiFile)
          }
        }
      }
      return runReadAction<String> { psiFile.text }
    }

    private fun PsiFile.applyLocalInspection(inspection: LocalInspectionTool): List<ProblemDescriptor> {
      val problems = mutableListOf<ProblemDescriptor>()
      val inspectionManager = InspectionManager.getInstance(project)
      ProgressManager.getInstance().executeProcessUnderProgress(
        {
          problems.addAll(
            runReadAction<List<ProblemDescriptor>> {
              inspection.processFile(this, inspectionManager)
            })
        },
        DaemonProgressIndicator()
      )
      return problems
    }

    fun String.createPsiFileForSolution(project: Project, language: Language): PsiFile = PsiFileFactory.getInstance(project).createFileFromText(
      "solution", language, this
    )

    private const val MAX_BODY_LINES_IN_SHORT_FUNCTION = 3
    private const val CODE_HINT_PSI_FILE_NAME = "codeHintPsiFile"
    private const val CODE_PSI_FILE_NAME = "codePsiFile"
    private const val HTML_HINT_CLASS_NAME = "hint"
  }
}
