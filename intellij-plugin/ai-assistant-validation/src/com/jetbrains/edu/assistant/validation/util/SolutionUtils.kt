package com.jetbrains.edu.assistant.validation.util

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.updateContent
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

const val TARGET_FILE_NAME_FOR_SOLUTIONS = "Main.kt"

// Propagate the author solution from the previous step to be able to update implemented functions for prompts
fun propagateAuthorSolution(previousTask: Task, currentTask: Task, project: Project) {
  replaceTaskFilesWithSolutions(currentTask, project) { fileName ->
    previousTask.taskFiles[fileName]?.getSolution()
  }
}

fun replaceTaskFilesWithSolutions(task: Task, project: Project, solutionProducer: (String) -> (String?)) {
  task.taskFiles.filter { !it.value.isTestFile && it.value.isVisible }.forEach { (k, f) ->
    solutionProducer(k)?.let { solution ->
      f.updateContent(project, solution)
    }
  }
}

fun <K> parseCsvFile(path: Path?, recordConverter: (CSVRecord) -> K): List<K>? {
  if (path != null && path.exists()) {
    Files.newBufferedReader(path).use { reader ->
      val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
      return csvParser.records.map(recordConverter)
    }
  }
  return null
}

fun downloadSolution(task: Task, project: Project, studentCode: String) {
  replaceTaskFilesWithSolutions(task, project) { fileName ->
    if (fileName.endsWith(TARGET_FILE_NAME_FOR_SOLUTIONS)) {
      studentCode
    } else {
      null
    }
  }
}
