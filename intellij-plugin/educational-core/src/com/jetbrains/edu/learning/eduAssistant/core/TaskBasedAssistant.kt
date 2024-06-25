package com.jetbrains.edu.learning.eduAssistant.core

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.getFunctionSignaturesFromGeneratedCode
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformAdapter
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformException
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_CODE_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_TEXT_HINT
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.eduAssistant.log.Loggers
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TaskBasedAssistant : Assistant {
  override suspend fun getHint(taskProcessor: TaskProcessor, state: EduState, userCode: String?): AssistantResponse {
    val task = taskProcessor.task
    logEduAssistantInfo(taskProcessor, "Next step hint request")
    Loggers.hintTimingLogger.info("Starting getHint function for task-id: ${task.id}")

    return try {
      if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return AssistantResponse(
         assistantError = AssistantError.NoCompiledCode
      )

      Loggers.hintTimingLogger.info("Retrieving the student's code for task-id: ${task.id}")
      var codeStr = ""
      userCode?.let {
        codeStr = it
      } ?: ApplicationManager.getApplication().invokeAndWait {
        codeStr = taskProcessor.getSubmissionTextRepresentation(state) ?: ""
      }
      logEduAssistantInfo(
        taskProcessor,
        """User code:
          |$codeStr
        """.trimMargin()
      )

      val nextStepCodeHintPrompt = generateCodeHintPrompt(taskProcessor, null, codeStr)
      val codeHint = generateCodeHint(taskProcessor, state, nextStepCodeHintPrompt, codeStr)
      return generateFinalHintsAndResponse(taskProcessor, codeHint, codeStr, nextStepCodeHintPrompt, state)
    }
    // TODO: Handle more exceptions with AiPlatformException
    catch (e: AiPlatformException) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      AssistantResponse(assistantError = e.assistantError)
    }
    catch (e: Throwable) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      AssistantResponse(assistantError = AssistantError.UnknownError)
    }
  }

  private fun getEnhancedCodeHint(taskProcessor: TaskProcessor, functionName: String, codeStr: String, codeHint: String, state: EduState): String {
    val task = taskProcessor.task
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    Loggers.hintTimingLogger.info("Retrieving the code hint from solution if it is a short function for task-id: ${task.id}")
    val nextStepCodeFromSolution = taskProcessor.getShortFunctionFromSolutionIfRecommended(codeHint, project, languageId, functionName, state.taskFile)
    if (nextStepCodeFromSolution != null) {
      logEduAssistantInfo(
        taskProcessor,
        """Code hint from solution:
             |$nextStepCodeFromSolution
          """.trimMargin()
      )
      return nextStepCodeFromSolution
    } else {
      Loggers.hintTimingLogger.info("Retrieving the function psi from student code for task-id: ${task.id}")
      val functionFromCode = taskProcessor.getFunctionPsiWithName(codeStr, functionName, project, languageId)
      Loggers.hintTimingLogger.info("Retrieving the function psi from code hint for task-id: ${task.id}")
      val functionFromCodeHint = taskProcessor.getFunctionPsiWithName(codeHint, functionName, project, languageId)
      Loggers.hintTimingLogger.info("Reducing the code hint for task-id: ${task.id}")
      val reducedCodeHint = taskProcessor.reduceChangesInCodeHint(functionFromCode?.copy(), functionFromCodeHint?.copy(), project, languageId)
      Loggers.hintTimingLogger.info("Applying inspections to the code hint for task-id: ${task.id}")
      //TODO: investigate wrong cases
      val nextStepCodeHint = applyInspections(reducedCodeHint, project, languageId)
      logEduAssistantInfo(
        taskProcessor,
        """Final code hint:
             |$nextStepCodeHint
          """.trimMargin()
      )
      return nextStepCodeHint
    }
  }

  private fun generateCodeHintPrompt(taskProcessor: TaskProcessor, nextStepTextHint: String?, codeStr: String): String {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the next step code hint prompt for task-id: ${task.id}")
    val language = task.course.languageDisplayName.lowercase()
    val nextStepCodeHintPrompt = nextStepTextHint?.let {
      buildNextStepCodeHintPromptFromTextHint(taskProcessor, it, codeStr, language)
    } ?: run {
      val hintContext = buildHintContext(taskProcessor)
      buildNextStepCodeHintPrompt(taskProcessor, codeStr, language, hintContext)
    }
    logEduAssistantInfo(
      taskProcessor,
      """Code hint prompt:
             |$nextStepCodeHintPrompt
          """.trimMargin()
    )
    return nextStepCodeHintPrompt
  }

  private suspend fun generateCodeHint(taskProcessor: TaskProcessor, state: EduState, nextStepCodeHintPrompt: String, codeStr: String): String? {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the code hint for task-id: ${task.id}")
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    val codeHint = taskProcessor.extractRequiredFunctionsFromCodeHint(
      getNextStepCodeHint(nextStepCodeHintPrompt, project, languageId),
      state.taskFile
    ).also {
      logEduAssistantInfo(
        taskProcessor,
        """Code hint response (before applying inspections):
            |$it
          """.trimMargin()
      )
      Loggers.hintTimingLogger.info("Received the code hint for task-id: ${task.id}")
    }
    if (codeHint.isBlank()) {
      return null
    }
    try {
      Loggers.hintTimingLogger.info("Retrieving the modified function name for task-id: ${task.id}")
      val functionName = taskProcessor.getModifiedFunctionNameInCodeHint(codeStr, codeHint)
      return getEnhancedCodeHint(taskProcessor, functionName, codeStr, codeHint, state)
    } catch (e: IllegalStateException) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      return null
    }
  }

  private suspend fun generateFinalHintsAndResponse(
    taskProcessor: TaskProcessor,
    codeHint: String?,
    codeStr: String,
    codeHintPrompt: String,
    state: EduState,
  ): AssistantResponse {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the text hint prompt for task-id: ${task.id}")
    val nextStepTextHintPrompt = codeHint?.let {
      buildNextStepTextHintPrompt(it, codeStr)
    } ?: run {
      logEduAssistantInfo(taskProcessor, "The code hint was not generated, so the text hint is generated first")
      val hintContext = buildHintContext(taskProcessor)
      buildNextStepTextHintPromptIfNoCodeHintIsGenerated(taskProcessor, codeStr, hintContext)
    }
    logEduAssistantInfo(
      taskProcessor,
      """Text hint prompt:
          |$nextStepTextHintPrompt
        """.trimMargin()
    )
    Loggers.hintTimingLogger.info("Retrieving the text hint for task-id: ${task.id}")
    val nextStepTextHint = AiPlatformAdapter.chat(userPrompt = nextStepTextHintPrompt, generationContextProfile = NEXT_STEP_TEXT_HINT)
    logEduAssistantInfo(
      taskProcessor,
      """Text hint response:
          |$nextStepTextHint
        """.trimMargin()
    )
    Loggers.hintTimingLogger.info("Received the text hint for task-id: ${task.id}")

    val nextStepCodeHintPrompt = codeHint?.let { codeHintPrompt } ?: run {
      generateCodeHintPrompt(taskProcessor, nextStepTextHint, codeStr)
    }
    val nextStepCodeHint = codeHint ?: run {
      logEduAssistantInfo(taskProcessor, "The code hint is generated for the second time")
      generateCodeHint(taskProcessor, state, nextStepCodeHintPrompt, codeStr)
    }

    val prompts = mapOf(
      "nextStepTextHintPrompt" to nextStepTextHintPrompt,
      "nextStepCodeHintPrompt" to nextStepCodeHintPrompt
    )
    return AssistantResponse(nextStepTextHint, nextStepCodeHint?.let { taskProcessor.applyCodeHint(it, state.taskFile) }, prompts).also {
      Loggers.hintTimingLogger.info("Completed getHint function for task-id: ${task.id}")
    }
  }

  private fun buildNextStepTextHintPrompt(nextStepCodeHint: String, codeStr: String) = """
    Based on the given code and the improved version of the code, provide a concise textual hint that directly guides to improve the given code.
    Here is the current code and the improved version of the code, all delimited with <>:
    
    The code:
    <$codeStr>
    
    The improved version of the code:
     <$nextStepCodeHint>
    
    Respond with a brief textual instruction in imperative form of what modifications need to be made to the code to achieve the improvements exhibited in the improved code. 
    Do not write any code, except names of functions or string literals. 
    Avoid explaining why the modifications are needed.
  """.trimIndent()

  private fun buildNextStepTextHintPromptIfNoCodeHintIsGenerated(
     taskProcessor: TaskProcessor,
     codeStr: String,
     hintContext: HintContext
  ) = """
    Based on a coding problem, determine the next step that must be taken to complete the task.
    Here is the list of steps that solves the problem and the code, all delimited with <>:
    
    Task: <${hintContext.taskStr}>
    
    Set of functions that might be implemented: <${hintContext.functionsSetStrFromAuthorSolution}>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <${hintContext.functionsSetStrFromUserSolution}>
    
    Hints: <${hintContext.hintsStr}>
    
    Theory: <${hintContext.theoryStr}>
    
    The solution can use only these strings: <${hintContext.availableForSolutionStrings ?: "None"}>
    
    The code:
    <$codeStr>
    
    ${buildTaskErrorInformation(taskProcessor)}
    
    Respond with a brief textual instruction in imperative form of what to do next in the problem-solving sequence.
    The response should not provide the complete solution, but instead focus on the next concise step that needs to be taken to make progress.
    Don't use the number of step in the response. Do not write any code, except names of functions that can be used in the solution.
  """.trimIndent()

  private fun buildTaskErrorInformation(taskProcessor: TaskProcessor) = if (taskProcessor.taskHasErrors()) {
    """
      The present code does not pass test <${taskProcessor.getFailedTestName()}>, the error message is <${taskProcessor.getFailureMessage()}>.
      ${
      if (taskProcessor.getExpectedValue() != null && taskProcessor.getActualValue() != null) {
        """
          The expected test output was:
          ${taskProcessor.getExpectedValue()}
          But the actual test output was:
          ${taskProcessor.getActualValue()}
        """.trimIndent()
      }
      else ""
    }
      If the task appears to be completed, provide a step-by-step guide that explains what changes need to be made to the code to fix the failing test.
    """.trimIndent()
  }
  else {
    ""
  }

  private fun formatCodeResponsePrompt(description: String, codeStr: String, language: String, taskProcessor: TaskProcessor) = """
    $description
    
    The student's code:
    <$codeStr>
    
    ${buildTaskErrorInformation(taskProcessor)}
    
    Write the response in the following format:
    Response: <non-code text>
    Code: ```$language
    <code>
    ```
    
    The code response should include the entire function or code block with the new changes incorporated into it.
  """.trimIndent()

  private fun buildNextStepCodeHintPrompt(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String,
    hintContext: HintContext
  ) = formatCodeResponsePrompt("""
    Generate a modified version of the provided student's code that incorporates the next step towards the solution for the described coding task. 
    The modified code should not be a complete solution but should represent the next logical step that the student needs to take to solve the task. 
    Try to maintain the original structure of the student's code and focus especially on addressing common errors, while guiding the student towards the correct implementation.
    Here is information about the task and the student's code, all blocks delimited with <>:
    
    Task: <${hintContext.taskStr}>
    
    Set of functions that might be implemented: <${hintContext.functionsSetStrFromAuthorSolution}>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <${hintContext.functionsSetStrFromUserSolution}>
    
    Hints: <${hintContext.hintsStr}>
    
    Theory: <${hintContext.theoryStr}>
    
    The solution can use only these strings: <${hintContext.availableForSolutionStrings ?: "None"}>

  """.trimIndent(), codeStr, language, taskProcessor)

  private fun buildNextStepCodeHintPromptFromTextHint(
    taskProcessor: TaskProcessor, nextStepTextHint: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Implement the next step to solve the coding task in the present code.
    Here is the description of the next step and the code, all delimited with <>:
    
    Next step: <$nextStepTextHint>
  """.trimIndent(), codeStr, language, taskProcessor)

  private suspend fun getNextStepCodeHint(nextStepCodeHintPrompt: String, project: Project, language: Language, maxAttempts: Int = 3): String {
    return withContext(Dispatchers.Default) {
      var lastNextStepCodeHint: String? = null
      repeat(maxAttempts) { _ ->
        val nextStepCodeHint = AiPlatformAdapter.chat(
          userPrompt = nextStepCodeHintPrompt,
          temp = 0.1,
          generationContextProfile = NEXT_STEP_CODE_HINT
        )
        if (lastNextStepCodeHint == nextStepCodeHint) {
          error("Failed to generate the code hint.")
        }
        val result = getCodeFromResponse(nextStepCodeHint, language.displayName.lowercase())
        if (!result.isNullOrBlank() && getFunctionSignaturesFromGeneratedCode(result, project, language).isNotEmpty()) {
          return@withContext result
        }
        lastNextStepCodeHint = nextStepCodeHint
      }
      error("Failed to generate the code hint.")
    }
  }

  private fun getCodeFromResponse(response: String, language: String): String? {
    val pattern = """```$language(.*?)```""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
    return pattern.find(response)?.groups?.get(1)?.value?.trim()
  }

  inner class HintContext(
    val taskStr: String,
    val functionsSetStrFromUserSolution: String?,
    val functionsSetStrFromAuthorSolution: String?,
    val hintsStr: String,
    val theoryStr: String,
    val availableForSolutionStrings: String?,
    val language: String,
    val taskProcessor: TaskProcessor
  ) {
    init {
      log()
    }

    fun log() {
      log("TASK TEXT", taskStr)
      log("FUNCTIONS SET FROM USER SOLUTION", functionsSetStrFromUserSolution)
      log("FUNCTIONS SET FROM AUTHOR SOLUTION", functionsSetStrFromAuthorSolution)
      log("TASK HINTS", hintsStr)
      log("THEORY SUMMARY", theoryStr)
      log("STRINGS SET", availableForSolutionStrings)
    }

    private fun log(name: String, value: String?) {
      value?.let {
        logEduAssistantInfo(
          taskProcessor,
          """Hint context - $name:
            |$it
          """.trimMargin()
        )
      }
    }
  }

  private fun buildHintContext(taskProcessor: TaskProcessor): HintContext {
    val task = taskProcessor.task
    val functionsSetStrFromUserSolution = taskProcessor.getFunctionsFromTask()
    val functionsSetStrFromAuthorSolution = task.authorSolutionContext?.functionSignatures?.filterNot {
      functionsSetStrFromUserSolution?.contains(
        it
      ) == true
    }
    val userStrings = taskProcessor.getStringsFromTask()
    val availableForSolutionStrings = task.authorSolutionContext?.functionsToStringMap?.values?.flatten()?.filterNot {
      userStrings.contains(
        it
      )
    }

    return HintContext(
      taskStr = taskProcessor.getTaskTextRepresentation(),
      functionsSetStrFromUserSolution = functionsSetStrFromUserSolution?.joinToString(separator = System.lineSeparator()),
      functionsSetStrFromAuthorSolution = functionsSetStrFromAuthorSolution?.joinToString(separator = System.lineSeparator()),
      hintsStr = taskProcessor.getHintsTextRepresentation().joinToString(separator = System.lineSeparator()),
      theoryStr = taskProcessor.getTheoryTextRepresentation(),
      availableForSolutionStrings = availableForSolutionStrings?.joinToString(separator = System.lineSeparator()),
      language = task.course.languageDisplayName.lowercase(),
      taskProcessor = taskProcessor
    )
  }

  private fun logEduAssistantInfo(taskProcessor: TaskProcessor, message: String) = Loggers.eduAssistantLogger.info(
    """Lesson id: ${taskProcessor.task.lesson.id}    Task id: ${taskProcessor.task.id}
      |$message
      |
    """.trimMargin()
  )
}