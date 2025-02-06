package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.StderrAnalyzer
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus

class RsStderrAnalyzer : StderrAnalyzer {
  override fun tryToGetCheckResult(stderr: String): CheckResult? = if (stderr.contains(COMPILATION_ERROR_MESSAGE, true)) {
    CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, stderr)
  }
  else {
    null
  }
}