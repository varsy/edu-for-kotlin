package com.jetbrains.edu.aiHints.python

import com.jetbrains.edu.aiHints.core.TaskProcessorImpl.Companion.applyInspections
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class PyInspectionProviderTest(
  private val codeToFix: String,
  private val expectedResult: String,
) : EduTestCase() {
  @Test
  fun `test applying inspections`() {
    assertEquals(expectedResult, applyInspections(codeToFix, project, PythonLanguage.INSTANCE))
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<String>> = listOf(
      PY_ARGUMENT_LIST_INSPECTION_TEST,
      PY_CHAINED_COMPARISONS_INSPECTION_TEST,
      PY_COMPARISON_WITH_NONE_TEST,
      PY_DICT_CREATION_INSPECTION_TEST,
      PY_EXCEPTION_INHERIT_TEST,
      PY_LIST_CREATION_TEST,
      PY_METHOD_PARAMETERS_TEST,
      PY_NONE_FUNCTION_ASSIGNMENT_TEST,
      PY_REDUNDANT_PARENTHESES_TEST,
      PY_SIMPLIFY_BOOLEAN_CHECK_TEST,
      PY_TRAILING_SEMICOLON_TEST,
    )

    private val PY_ARGUMENT_LIST_INSPECTION_TEST = arrayOf(
      """
        def foo(a, b):
          return a + b
  
        foo(12, 24, 36)
      """.trimIndent(), """
        def foo(a, b):
          return a + b

        foo(12, 24)
      """.trimIndent()
    )

    private val PY_CHAINED_COMPARISONS_INSPECTION_TEST = arrayOf(
      """
        def do_comparison(x):
          min = 10
          max = 100
          if x >= min and x <= max:
            pass
      """.trimIndent(),
      """
        def do_comparison(x):
          min = 10
          max = 100
          if min <= x <= max:
            pass
      """.trimIndent()
    )

    private val PY_COMPARISON_WITH_NONE_TEST = arrayOf(
      """
        a = 2
        if a == None:
          print("Success")
      """.trimIndent(),
      """
        a = 2
        if a is None:
          print("Success")
      """.trimIndent()
    )

    private val PY_DICT_CREATION_INSPECTION_TEST = arrayOf(
      """
       dict = {}
       dict['var'] = 1
      """.trimIndent(),
      """
        dict = {'var': 1}
        
      """.trimIndent()
    )

    private val PY_EXCEPTION_INHERIT_TEST = arrayOf(
      """
        class A:
          pass

        def me_exception():
          raise A()
      """.trimIndent(),
      """
        class A(Exception):
          pass

        def me_exception():
          raise A()
      """.trimIndent()
    )

    private val PY_LIST_CREATION_TEST = arrayOf(
      """
        l = [1]
        l.append(2)
      """.trimIndent(),
      """
        l = [1, 2]

      """.trimIndent()
    )

    private val PY_METHOD_PARAMETERS_TEST = arrayOf(
      """
        class Movie:
          def show():
             pass
      """.trimIndent(),
      """
        class Movie:
          def show(self):
             pass
      """.trimIndent()
    )

    private val PY_NONE_FUNCTION_ASSIGNMENT_TEST = arrayOf(
      """
        def just_print():
          print("Hello!")

        action = just_print()
      """.trimIndent(),
      """
        def just_print():
          print("Hello!")


        just_print()
      """.trimIndent()
    )

    private val PY_REDUNDANT_PARENTHESES_TEST = arrayOf(
      """
        a = 5
        b = 3
        if (a + b):
          pass
      """.trimIndent(),
      """
        a = 5
        b = 3
        if a + b:
          pass
      """.trimIndent()
    )

    private val PY_SIMPLIFY_BOOLEAN_CHECK_TEST = arrayOf(
      """
        def func(s):
          if s.isdigit() == True:
            return int(s)
      """.trimIndent(),
      """
        def func(s):
          if s.isdigit():
            return int(s)
      """.trimIndent()
    )

    private val PY_TRAILING_SEMICOLON_TEST = arrayOf(
      """
        def my_func(a):
          c = a ** 2;
          return c
      """.trimIndent(),
      """
        def my_func(a):
          c = a ** 2
          return c
      """.trimIndent()
    )
  }
}