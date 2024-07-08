package com.jetbrains.edu.learning.courseFormat.checkio

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class CheckiOMission : EduTask() {
  @Transient // used to get missions from server
  var station: CheckiOStation = CheckiOStation()
  var code = ""
  var slug = ""
  var secondsFromLastChangeOnServer: Long = 0

  fun getTaskFile(): TaskFile {
    val taskFiles = taskFiles.values
    require(taskFiles.isNotEmpty())
    return taskFiles.first()
  }

  override var status: CheckStatus
    get() = super.status
    set(status) {
      if (checkStatus == CheckStatus.Unchecked) {
        checkStatus = status
      }
      else if (checkStatus == CheckStatus.Failed && status == CheckStatus.Solved) {
        checkStatus = CheckStatus.Solved
      }
    }

  override val isToSubmitToRemote: Boolean
    get() = false
  override val itemType: String
    get() = CHECK_IO_MISSION_TASK_TYPE

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    return id == (other as CheckiOMission).id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return "CheckiOMission{" +
           "id=" + id +
           ", stationId=" + station.id +
           ", stationName='" + station.name + "'" +
           ", title='" + name + "'" +
           ", secondsPast=" + secondsFromLastChangeOnServer +
           "}"
  }

  companion object {
    const val CHECK_IO_MISSION_TASK_TYPE = "checkiO"
  }
}