package com.jetbrains.edu.aiHints.kotlin

import com.jetbrains.edu.aiHints.core.FunctionSignatureResolver
import com.jetbrains.edu.aiHints.core.TaskProcessorImpl.Companion.createPsiFileForSolution
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.kotlin.courses.createKotlinCourse
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.junit.Test

class FunctionSignatureResolverTest : JdkCheckerTestBase() {
  @Test
  fun testGetFunctionBySignatureBasic() {
    val functionSignature = FunctionSignature(
      "add",
      listOf(FunctionParameter("a", "Int"), FunctionParameter("b", "Int")),
      "Int",
      SignatureSource.MODEL_SOLUTION
    )
    getFunctionBySignature(basicFunction, functionSignature)
  }

  @Test
  fun testGetFunctionBySignatureWithNoReturn() {
    val functionSignature = FunctionSignature(
      "getPictureWidth",
      listOf(FunctionParameter("picture", "String")),
      "String",
      SignatureSource.MODEL_SOLUTION
    )
    getFunctionBySignature(noReturnFunction, functionSignature)
  }

  @Test
  fun testGetFunctionBySignatureWithFunctionReturnValue() {
    val functionSignature = FunctionSignature("getPrinter", emptyList(), "Function0<Unit>", SignatureSource.MODEL_SOLUTION)
    getFunctionBySignature(functionReturnValue, functionSignature)
  }

  @Test
  fun testGetFunctionBySignatureWithNullableReturnValue() {
    val functionSignature = FunctionSignature(
      "nullableLength",
      listOf(FunctionParameter("s", "String?")),
      "Int?",
      SignatureSource.MODEL_SOLUTION
    )
    getFunctionBySignature(nullableReturnValue, functionSignature)
  }

  private fun getFunctionBySignature(code: String, functionSignature: FunctionSignature) {
    val psiFile = code.createPsiFileForSolution(project, language)
    assertEquals(code, FunctionSignatureResolver.getFunctionBySignature(psiFile, functionSignature.name, language)?.text)
  }

  override fun createCourse(): Course = createKotlinCourse()

  companion object {
    private val basicFunction = """
      fun add(a: Int, b: Int): Int {
          return a + b
      }
    """.trimIndent()

    private val noReturnFunction = """
      fun getPictureWidth(picture: String) = picture.lines().maxOfOrNull { it.length } ?: 0
    """.trimIndent()

    private val functionReturnValue = """
      fun getPrinter(): () -> Unit = { println("Printing...") }
    """.trimIndent()

    private val nullableReturnValue = """
      fun nullableLength(s: String?) = s?.length
    """.trimIndent()
  }
}