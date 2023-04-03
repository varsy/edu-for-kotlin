package com.jetbrains.edu.learning.courseFormat.tasks.data

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.runInWriteActionAndWait
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.util.*

class DataTask : Task {
  @get: Synchronized
  @set: Synchronized
  var attempt: DataTaskAttempt? = null

  @get: Synchronized
  val isTimeLimited: Boolean
    get() = attempt?.endDateTime != null

  //used for deserialization
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = DATA_TASK_TYPE

  override val supportSubmissions: Boolean
    get() = false

  fun isRunning(): Boolean {
    if (status != CheckStatus.Unchecked) return false
    return attempt?.isRunning ?: false
  }

  @Throws(IOException::class)
  fun getOrCreateDataset(project: Project, input: String): VirtualFile {
    val taskDir = getDir(project.courseDir) ?: error("Unable to find task directory")
    val dataset = runReadAction {
      taskDir.findFileByRelativePath(datasetFilePath)
    }
    if (dataset == null) {
      return GeneratorUtils.createTextChildFile(project, taskDir, datasetFilePath, input)
             ?: error("File $datasetFilePath can't be created")
    }

    val datasetDocument = runReadAction {
      FileDocumentManager.getInstance().getDocument(dataset)
    } ?: error("Can't get document of dataset file - ${dataset.path}")
    if (datasetDocument.text != input) {
      runInWriteActionAndWait {
        VfsUtil.saveText(dataset, input)
        FileDocumentManager.getInstance().reloadFromDisk(datasetDocument)
      }
    }
    return dataset
  }

  companion object {
    @NonNls
    const val DATA_TASK_TYPE: String = "dataset"

    @NonNls
    const val DATA_FOLDER_NAME: String = "data"

    @NonNls
    const val DATA_SAMPLE_FOLDER_NAME: String = "sample"

    @NonNls
    const val DATASET_FOLDER_NAME: String = "dataset"

    @NonNls
    const val INPUT_FILE_NAME: String = "input.txt"

    private val datasetFilePath: String = GeneratorUtils.joinPaths(DATA_FOLDER_NAME, DATASET_FOLDER_NAME, INPUT_FILE_NAME)

    fun HyperskillStepSource.isDataTask(): Boolean = block?.name == DATA_TASK_TYPE
  }
}