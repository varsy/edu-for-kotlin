package com.jetbrains.edu.learning.eduAssistant.utils

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task


// Only for the Kotlin Onboarding Introduction: https://plugins.jetbrains.com/plugin/21067-kotlin-onboarding-introduction and for Edu tasks
fun isNextStepHintApplicable(task: Task) = task.course.id == 21067 && task is EduTask

fun isGetHintButtonShown(task: Task) = isNextStepHintApplicable(task) && task.course.courseMode == CourseMode.STUDENT && task.status == CheckStatus.Failed // TODO: when should we show this button?
