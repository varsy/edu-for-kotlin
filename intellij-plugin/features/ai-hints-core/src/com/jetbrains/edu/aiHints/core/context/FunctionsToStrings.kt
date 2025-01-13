package com.jetbrains.edu.aiHints.core.context

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.EduAIHintsProvider
import com.jetbrains.edu.aiHints.core.TaskProcessorImpl.Companion.createPsiFileForSolution
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getSolution

/**
 * If the function does not contain any strings, then associated list is empty
 */
@JvmInline
value class FunctionsToStrings(val value: Map<FunctionSignature, List<String>>) {
  fun mapOfStringSignatures(): Map<String, List<String>> =
    value
      .mapKeys { it.key.toString() }
      .filter { it.key.isNotEmpty() }

  fun signatures(): Set<FunctionSignature> = value.keys

  companion object {
    @JvmStatic
    @RequiresReadLock
    fun create(project: Project, language: Language, files: List<TaskFile>): FunctionsToStrings {
      val course = project.course ?: error("Course is null for project $project")
      val functionsToStrings = files.asSequence()
        .flatMap { file ->
          val psiFileSolution = file.getSolution().createPsiFileForSolution(project, language)
          val functionsToStringsMap = EduAIHintsProvider.forCourse(course)?.getFunctionsToStringsMap(psiFileSolution) ?: return@flatMap emptySequence()
          functionsToStringsMap.value.entries.asSequence()
        }
        .associate { it.toPair() }
      return FunctionsToStrings(functionsToStrings)
    }
  }
}