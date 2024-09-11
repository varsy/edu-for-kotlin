package com.jetbrains.edu.cognifire.tutorial

data class TutorialTask(val name: String, val type: TaskType)

enum class TaskType {
  THEORY_TASK,
  EDU_TASK,
}
