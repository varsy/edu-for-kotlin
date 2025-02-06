package com.jetbrains.edu.python.learning.checker

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.StderrAnalyzer
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus

class PyStderrAnalyzer : StderrAnalyzer {
  private val SYNTAX_ERRORS = listOf("SyntaxError", "IndentationError", "TabError", "NameError")
  private val RUNTIME_ERRORS = listOf("Traceback (most recent call last):")

  override fun tryToGetCheckResult(stderr: String): CheckResult? =
    when {
      SYNTAX_ERRORS.any { it in stderr } -> CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, stderr)
      RUNTIME_ERRORS.any { it in stderr } -> CheckResult(CheckStatus.Failed, CheckUtils.EXECUTION_ERROR_MESSAGE, stderr)
      else -> null
    }

  override fun getStackTrace(stderr: String): List<Pair<String, Int>> {
    val regex = Regex("""File "(.+)", line (\d+)""")
    val matches = regex.findAll(stderr).map { it.destructured }.toList()
    return matches.map { (fileName, lineNumber) -> fileName to lineNumber.toInt() }
  }
}