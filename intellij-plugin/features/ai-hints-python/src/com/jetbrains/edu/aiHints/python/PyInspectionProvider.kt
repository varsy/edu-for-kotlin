package com.jetbrains.edu.aiHints.python

import com.jetbrains.edu.aiHints.core.InspectionProvider

class PyInspectionProvider : InspectionProvider {

  /**
   * @see <a href=https://www.jetbrains.com/help/inspectopedia/Python.html>Python Inspections</a>
   * @see <a href=https://github.com/JetBrains-Research/code-quality-ij-server/blob/master/ij-core/src/main/kotlin/org/jetbrains/research/ij/headless/server/inspector/configs/python/PythonIJCodeInspectorConfig.kt#L7>Ignored inspections</a>
   */
  override val inspections: Set<String>
    get() = setOf(
      "PyArgumentList",  // Quick-Fix is available in case of unexpected argument
      "PyChainedComparisons",
      "PyComparisonWithNone",
      "PyDictCreation",
      "PyExceptionInherit",
      "PyListCreation",
      "PyMethodParameters",
      "PyNoneFunctionAssignment",
      "PyRedundantParentheses",
      "PySimplifyBooleanCheck",
      "PyTrailingSemicolon",
    )
}