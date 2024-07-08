@file:JvmName("StepikConnectorUtils")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

fun loadAndFillLessonAdditionalInfo(lesson: Lesson, course: Course? = null) {
  val attachmentLink = "${getStepikUrl()}/media/attachments/lesson/${lesson.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(attachmentLink) ?: return
  val lessonInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, LessonAdditionalInfo::class.java)

  lesson.customPresentableName = lessonInfo.customName
  lessonInfo.tasksInfo.forEach { (id, task) ->
    lesson.getTaskById(id)?.apply {
      name = task.name
      customPresentableName = task.customName
      taskFiles = task.taskFiles.associateBy(TaskFile::name) { it }
    }
  }

  if (course is HyperskillCourse && lessonInfo.additionalFiles.isNotEmpty()) {
    course.additionalFiles = lessonInfo.additionalFiles
  }
}

fun loadAndFillAdditionalCourseInfo(course: Course, attachmentLink: String? = null) {
  val link = attachmentLink ?: "${getStepikUrl()}/media/attachments/course/${course.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(link) ?: return
  val courseInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, CourseAdditionalInfo::class.java)

  course.additionalFiles = courseInfo.additionalFiles
  course.solutionsHidden = courseInfo.solutionsHidden
}