package com.jetbrains.edu.kotlin.eduAssistant.validation

import com.jetbrains.edu.learning.LessonBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import org.jetbrains.kotlin.idea.KotlinLanguage

fun createKotlinOnboardingMockCourse() = course(language = KotlinLanguage.INSTANCE) {
  frameworkLesson(name = "TheFirstDateWithProgramming") {
    addTheoryTask(name = "Introduction")
    addTheoryTask(name = "programEntryPointTheory")
    addTheoryTask(name = "builtinFunctionsTheory")
    addTheoryTask(name = "variablesTheory")

    addEduTask(name = "Variables", listOf("RealQuestions.kt"))
  }

  frameworkLesson(name = "WarmUp") {
    addTheoryTask(name = "Introduction")
    addTheoryTask(name = "typesOfVariablesTheory")
    addTheoryTask(name = "customFunctionsTheory")
    addTheoryTask(name = "customFunctionsTheoryPartTwo")
    addTheoryTask(name = "todoFunction")

    addEduTask(name = "isCompleteFunction", listOf("Util.kt"))
  }

  frameworkLesson(name = "MastermindAdvanced") {
    val hidden = listOf("Util.kt")

    addTheoryTask(name = "introduction")
    addTheoryTask(name = "randomFunction")
    addTheoryTask(name = "joinToStringFunction")
    addTheoryTask(name = "errorHandling")

    addEduTask(name = "safeUserInputFunction", hidden)

    addEduTask(name = "CompleteTheProject", hidden)
  }

  frameworkLesson(name = "Hangman") {
    val hidden = listOf("Util.kt", "Words.kt")

    addTheoryTask(name = "CreateTheGame")

    addEduTask(name = "generateSecretFunction", hidden)

    addEduTask(name = "isCorrectInputFunction", hidden)

    addEduTask(name = "CompleteTheProject", hidden)
  }

  frameworkLesson(name = "AlmostDone") {
    val hidden = listOf("Images.kt", "PreDefinedSymbols.kt")

    addTheoryTask(name = "Introduction")
    addTheoryTask(name = "multiRowStringsTheory")
    addTheoryTask(name = "whenTheory")
    addTheoryTask(name = "errorFunction")
    addTheoryTask(name = "linesFunction")
    addTheoryTask(name = "repeatFunction")
    addTheoryTask(name = "stringBuilder")

    addEduTask(name = "StringFunctionsPartTwo", hidden)

    addTheoryTask(name = "nullValue")
    addTheoryTask(name = "nullSafetyTheory")
    addTheoryTask(name = "letScopeFunction")

    addEduTask(name = "choosePictureFunction", hidden)

    addEduTask(name = "NullSafetyPartTwo", hidden)
  }

  frameworkLesson(name = "LastPush") {
    val hidden = listOf("Patterns.kt", "PreDefinedSymbols.kt")

    addTheoryTask(name = "PatternsGenerator")

    addEduTask(name = "repeatHorizontallyFunction", hidden)

    addEduTask(name = "dropTopFromLineFunction", hidden)
  }
}

private fun getFileContentFromResources(path: String) = object {}.javaClass.getResource(path)?.readText()
                                                        ?: error("File from test resources not found: $path")

private fun LessonBuilder<FrameworkLesson>.addTheoryTask(name: String) {
  val lessonName = this.lesson.name
  val pathToTheoryFile = "KotlinOnboardingData/$lessonName/theory/$name.md"
  val theoryContent = getFileContentFromResources(pathToTheoryFile)
  // The contents of the file are not important, but the file must be added for correct initialisation
  val pathToTaskCode = "KotlinOnboardingData/theory/Main.kt"
  val taskCode = getFileContentFromResources(pathToTaskCode)
  theoryTask(name = name, taskDescription = theoryContent, taskDescriptionFormat = DescriptionFormat.MD) {
    kotlinTaskFile(name = "src/Main.kt", text = taskCode, visible = true)
  }
}

private fun LessonBuilder<FrameworkLesson>.addEduTask(name: String, hiddenFiles: List<String>) {
  val lessonName = this.lesson.name
  val pathToTaskDescription = "KotlinOnboardingData/$lessonName/tasks/$name/task.md"
  val taskDescription = getFileContentFromResources(pathToTaskDescription)
  val pathToTaskCode = "KotlinOnboardingData/$lessonName/tasks/$name/Main.kt"
  val taskCode = getFileContentFromResources(pathToTaskCode)
  // The contents of the tests are not important, but the file must be added to correctly run the checks from the tests
  val pathToTestCode = "KotlinOnboardingData/test/Test.kt"
  val testCode = getFileContentFromResources(pathToTestCode)
  eduTask(name = name, taskDescription = taskDescription, taskDescriptionFormat = DescriptionFormat.MD) {
    kotlinTaskFile(name = "src/Main.kt", text = taskCode, visible = true)
    hiddenFiles.forEach { hiddenFileName ->
      val pathToFile = "KotlinOnboardingData/$lessonName/hidden/$hiddenFileName"
      val fileContent = getFileContentFromResources(pathToFile)
      kotlinTaskFile(name = "src/$hiddenFileName", text = fileContent, visible = false)
    }
    kotlinTaskFile(name = "test/Test.kt", text = testCode, visible = false)
  }
}
