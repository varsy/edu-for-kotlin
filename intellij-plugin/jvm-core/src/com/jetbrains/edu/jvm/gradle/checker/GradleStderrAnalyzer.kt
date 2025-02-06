package com.jetbrains.edu.jvm.gradle.checker

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.StderrAnalyzer
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus

class GradleStderrAnalyzer : StderrAnalyzer {
  override fun tryToGetCheckResult(stderr: String): CheckResult? {
    for (error in CheckUtils.COMPILATION_ERRORS) {
      if (error in stderr) return CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, stderr)
    }
    return null
  }
}