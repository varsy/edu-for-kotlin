package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyConfigurator

/**
 * This class is needed as a hack to override behavior of base configurator during Hyperskill course creation
 *
 */
class PyHyperskillBaseConfigurator : PyConfigurator() {
  override fun getMockFileName(course: Course, text: String): String = MAIN_PY
}