package com.jetbrains.edu.learning.courseFormat.checkio

import com.jetbrains.edu.learning.courseFormat.Lesson
import org.jetbrains.annotations.NonNls

class CheckiOStation : Lesson() {
  fun addMission(mission: CheckiOMission) {
    addTask(mission)
  }

  fun getMission(id: Int): CheckiOMission? {
    val task = getTaskById(id)
    return if (task is CheckiOMission) task else null
  }

  val missions: List<CheckiOMission>
    get() = taskList.filterIsInstance(CheckiOMission::class.java)

  override val itemType: @NonNls String
    get() = "checkiO"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val station = other as CheckiOStation
    return id == station.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String {
    return "missions=[" + missions.joinToString("\n") { it.toString() } + "]"
  }
}
