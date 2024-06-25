package com.jetbrains.edu.jarvis.utils

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN

fun isJarvisApplicable(course: Course) = course.languageId == KOTLIN
