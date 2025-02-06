package com.jetbrains.edu.python.learning.checker

import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult

class PyCodeExecutor : DefaultCodeExecutor() {

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = PyStderrAnalyzer().tryToGetCheckResult(errorOutput)
}
