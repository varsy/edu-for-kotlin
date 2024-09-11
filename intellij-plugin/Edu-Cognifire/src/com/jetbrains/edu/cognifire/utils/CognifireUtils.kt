package com.jetbrains.edu.cognifire.utils

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN

fun isCognifireApplicable(course: Course) = course.languageId == KOTLIN
