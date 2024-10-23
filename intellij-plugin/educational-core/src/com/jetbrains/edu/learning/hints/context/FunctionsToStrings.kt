package com.jetbrains.edu.learning.hints.context

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.hints.StringExtractor
import com.jetbrains.edu.learning.hints.TaskProcessorImpl.Companion.createPsiFileForSolution

/**
 * If the function does not contain any strings, then associated list is empty
 */
@JvmInline
value class FunctionsToStrings(val value: Map<FunctionSignature, List<String>>) {

  fun mapOfStringSignatures() = value.mapKeys { it.key.toString() }.filter { it.key.isNotEmpty() }

  fun signatures(): Set<FunctionSignature> = value.keys

  companion object {

    @JvmStatic
    @RequiresReadLock
    fun create(project: Project, language: Language, files: List<TaskFile>): FunctionsToStrings {
      val functionsToStrings = files.asSequence()
        .flatMap { file ->
          val psiFileSolution = file.getSolution().createPsiFileForSolution(project, language)
          StringExtractor.getFunctionsToStringsMap(psiFileSolution, language).value.entries.asSequence()
        }
        .associate { it.toPair() }

      return FunctionsToStrings(functionsToStrings)
    }
  }
}
