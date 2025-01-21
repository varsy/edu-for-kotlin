package com.jetbrains.edu.aiHints.python

import com.jetbrains.edu.aiHints.core.FunctionSignatureResolver
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.createPsiFile
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class PyFunctionSignatureResolverTest(
  private val functionName: String,
  private val code: String
) : EduTestCase() {

  @Test
  fun `test getting function by signature`() {
    val psiFile = createPsiFile(project, code)
    val actualFunctionName = FunctionSignatureResolver.getFunctionBySignature(psiFile, functionName, PythonLanguage.INSTANCE)?.text
    assertEquals(code, actualFunctionName)
  }

  companion object {
    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<String>> = listOf(
      arrayOf("simple", """
            def simple():
                return "hello"
        """.trimIndent()),

      arrayOf("with_params", """
            def with_params(a, b, c):
                return a + b + c
        """.trimIndent()),

      arrayOf("default_params", """
            def default_params(x=10, y="default"):
                return x, y
        """.trimIndent()),

      arrayOf("typed", """
            def typed(x: int, y: str) -> bool:
                return len(y) == x
        """.trimIndent()),

      arrayOf("variable_args", """
            def variable_args(*args, **kwargs):
                return args, kwargs
        """.trimIndent()),

      arrayOf("async_func", """
            async def async_func():
                return await something()
        """.trimIndent()),

      arrayOf("decorated", """
            @decorator
            def decorated(x):
                return x
        """.trimIndent()),

      arrayOf("multi_decorated", """
            @decorator1
            @decorator2
            def multi_decorated():
                pass
        """.trimIndent()),

      arrayOf("documented", """
            def documented():
                \"\"\"This is a docstring\"\"\"
                return None
        """.trimIndent()),

      arrayOf("outer", """
            def outer():
                def inner():
                    return "inner"
                return inner()
        """.trimIndent())
    )
  }
}