package com.jetbrains.edu.learning.eduAssistant.processors

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.eduAssistant.context.SignatureSource
import com.jetbrains.edu.learning.eduAssistant.context.StringExtractor
import com.jetbrains.edu.learning.eduAssistant.context.differ.FilesDiffer
import com.jetbrains.edu.learning.eduAssistant.context.differ.FunctionDiffReducer
import com.jetbrains.edu.learning.eduAssistant.context.differ.filterAllowedModifications
import com.jetbrains.edu.learning.eduAssistant.context.differ.getChangedContent
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.*
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.getTextFromTaskTextFile
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

  companion object {
    private const val MAX_BODY_LINES_IN_SHORT_FUNCTION = 3
    private const val CODE_HINT_PSI_FILE_NAME = "codeHintPsiFile"
    private const val CODE_PSI_FILE_NAME = "codePsiFile"
    private const val HTML_HINT_CLASS_NAME = "hint"
  }
}
