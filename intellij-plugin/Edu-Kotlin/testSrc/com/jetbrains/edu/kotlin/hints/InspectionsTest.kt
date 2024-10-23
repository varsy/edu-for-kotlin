package com.jetbrains.edu.kotlin.hints

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.hints.courses.createKotlinCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.hints.TaskProcessorImpl.Companion.applyInspections
import org.junit.Test

class InspectionsTest : JdkCheckerTestBase() {

  @Test
  fun testInlineVariableInspection() {
    val code = """
      private fun logError(error: Exception, filePath: String) {
          val text = error
          PrintWriter(File(filePath), Charsets.UTF_8).use { it.print(text) }
      }
    """.trimIndent()
    val expected = """
      private fun logError(error: Exception, filePath: String) {
          PrintWriter(File(filePath), Charsets.UTF_8).use { it.print(error) }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testImplicitThisAndPublicApiImplicitTypeInspections() {
    val code = """
      class Foo {
          fun s() = ""
      
          fun test() {
              s()
          }
      }
    """.trimIndent()
    val expected = """
      class Foo {
          fun s(): String = ""

          fun test() {
              this.s()
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRedundantNullableReturnTypeInspection() {
    val code = """
      fun greeting(): String? = "Hello!"
    """.trimIndent()
    val expected = """
      fun greeting(): String = "Hello!"
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRedundantSemicolonInspection() {
    val code = """
      val myMap = mapOf("one" to 1, "two" to 2);
    """.trimIndent()
    val expected = """
      val myMap = mapOf("one" to 1, "two" to 2)
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testAddOperatorModifierInspection() {
    val code = """
      class Complex(val real: Double, val imaginary: Double) {
          fun plus(other: Complex) =
              Complex(real + other.real, imaginary + other.imaginary)
      }
    """.trimIndent()
    val expected = """
      class Complex(val real: Double, val imaginary: Double) {
          operator fun plus(other: Complex): Complex =
              Complex(this.real + other.real, this.imaginary + other.imaginary)
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testAddVarianceModifierInspection() {
    val code = """
      class Box<T>(val obj: T)

      fun consumeString(box: Box<String>) {}
      fun consumeCharSequence(box: Box<CharSequence>) {}

      fun usage(box: Box<String>) {
          consumeString(box)
          consumeCharSequence(box)
      }
    """.trimIndent()
    val expected = """
      class Box<out T>(val obj: T)

      fun consumeString(box: Box<String>) {}
      fun consumeCharSequence(box: Box<CharSequence>) {}

      fun usage(box: Box<String>) {
          consumeString(box)
          consumeCharSequence(box)
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRedundantIfInspection() {
    val code = """
      class Example {
          fun foo(): Boolean = true
          
          fun test(): Boolean {
              if (foo()) {
                 return true
              } else {
                 return false
              }
          }
      }
    """.trimIndent()
    val expected = """
      class Example {
          fun foo(): Boolean = true
          
          fun test(): Boolean {
              return this.foo()
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRemoveToStringInStringTemplateInspection() {
    val code = """
      class MyClass {
        fun foo(a: Int, b: Int): Int = a + b
        
        fun test(): String {
            return "Foo: ${'$'}{this.foo(0, 4).toString()}" 
        }
      }
    """.trimIndent()
    val expected = """
      class MyClass {
        fun foo(a: Int, b: Int): Int = a + b
        
        fun test(): String {
            return "Foo: ${'$'}{this.foo(0, 4)}" 
        }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testCanBeValInspection() {
    val code = """
      fun foo() {
          var list = listOf(1,2,3)
          println(list.size)
      }
    """.trimIndent()
    val expected = """
      fun foo() {
          val list = listOf(1,2,3)
          println(list.size)
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceGetOrSetInspection() {
    val code = """
      class Test {
          operator fun get(i: Int): Int = 0
      }

      fun test() {
          Test().get(0)
      }
    """.trimIndent()
    val expected = """
      class Test {
          operator fun get(i: Int): Int = 0
      }

      fun test() {
          Test()[0]
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testConvertReferenceToLambdaInspection() {
    val code = """
      fun checkIdentifier(id: String) {
          fun Char.isIdentifierStart() = this in 'A'..'z'
          fun Char.isIdentifierPart() = this.isIdentifierStart() || this in '0'..'9'

          if (!id.subSequence(1, id.length).all(Char::isIdentifierPart)) {
              print("Identifier should contain only letters and numbers")
          }
      }
    """.trimIndent()
    val expected = """
      fun checkIdentifier(id: String) {
          fun Char.isIdentifierStart() = this in 'A'..'z'
          fun Char.isIdentifierPart() = this.isIdentifierStart() || this in '0'..'9'
   
          if (!id.subSequence(1, id.length).all { it.isIdentifierPart() }) {
              print("Identifier should contain only letters and numbers")
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRemoveRedundantQualifierNameInspection() {
    val code = """
      package my.simple.name

      class Foo

      fun main() {
          val a = my.simple.name.Foo()
      }
    """.trimIndent()
    val expected = """
      package my.simple.name

      class Foo

      fun main() {
          val a = Foo()
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testConvertTwoComparisonsToRangeCheckInspection() {
    val code = """
      fun checkMonth(month: Int): Boolean {
          return month >= 1 && month <= 12
      }
    """.trimIndent()
    val expected = """
      fun checkMonth(month: Int): Boolean {
          return month in 1..12
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testIntroduceWhenSubjectInspection() {
    val code = """
      fun test(obj: Any): String {
          return when {
              obj is String -> "string"
              obj is Int -> "int"
              else -> "unknown"
          }
      }
    """.trimIndent()
    val expected = """
      fun test(obj: Any): String {
          return when (obj) {
              is String -> "string"
              is Int -> "int"
              else -> "unknown"
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testLoopToCallChainInspection() {
    val code = """
      fun foo(list: List<String>): List<Int> {
        val result = ArrayList<Int>()
        for (s in list) {
           if (s.length > 0)
             result.add(s.hashCode())
           }
        return result
      }
    """.trimIndent()
    val expected = """
      fun foo(list: List<String>): List<Int> {
          val result = ArrayList<Int>()
          list
              .filter { it.length > 0 }
              .forEach { result.add(it.hashCode()) }
          return result
      }
    """.trimIndent()
    assertEquals(expected.reformatCode(project), applyInspections(code, project, language).reformatCode(project))
  }

  @Test
  fun testMayBeConstantInspection() {
    val code = """
      val foo: Int = 1
    """.trimIndent()
    val expected = """
      const val foo: Int = 1
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testMoveVariableDeclarationIntoWhenInspection() {
    val code = """
      fun someCalc(x: Int) = x * 42

      fun foo(x: Int): Int {
        val a = someCalc(x)
        return when (a) {
          1 -> a
          2 -> 2 * a
          else -> 24
        }
      }
    """.trimIndent()
    val expected = """
      fun someCalc(x: Int): Int = x * 42
      
      fun foo(x: Int): Int {
          return when (val a = someCalc(x)) {
          1 -> a
          2 -> 2 * a
          else -> 24
        }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRedundantVisibilityModifierInspection() {
    val code = """
      class A {
        public fun testA() {
          val a = 0
        }
      }
    """.trimIndent()
    val expected = """
      class A {
        fun testA() {
          val a = 0
        }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRemoveCurlyBracesFromTemplateInspection() {
    val code = """
      fun redundant() {
         val x = 4
         val y = "${'$'}{x}" 
      }
    """.trimIndent()
    val expected = """
      fun redundant() {
         val x = 4
         val y = "${'$'}x" 
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceCallWithBinaryOperatorInspection() {
    val code = """
      fun test(): Boolean {
          return 2.compareTo(1) > 0
      }
    """.trimIndent()
    val expected = """
      fun test(): Boolean {
          return 2 > 1
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceManualRangeWithIndicesCallsInspection() {
    val code = """
      fun main(args: Array<String>) {
          for (index in 0..args.size - 1) {
              println(args[index])
          }
      }
    """.trimIndent()
    val expected = """
      fun main(args: Array<String>) {
          for (index in args.indices) {
              println(args[index])
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceRangeToWithUntilInspection() {
    val code = """
      fun foo(a: Int) {
          for (index in 0..a - 1) {
              println(index)
          }
      }
    """.trimIndent()
    val expected = """
      fun foo(a: Int) {
          for (index in 0 until a) {
              println(index)
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testLiftReturnOrAssignmentInspection() {
    val code = """
      fun foo(arg: Int): String {
          when (arg) {
              0 -> return "Zero"
              1 -> return "One"
              else -> return "Multiple"
          }
      }
    """.trimIndent()
    val expected = """
      fun foo(arg: Int): String {
          return when (arg) {
              0 -> "Zero"
              1 -> "One"
              else -> "Multiple"
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testRemoveSingleExpressionStringTemplateInspection() {
    val code = """
      const val x: String = "Hello"
      const val y: String = "${'$'}x"
    """.trimIndent()
    val expected = """
      const val x: String = "Hello"
      const val y: String = x
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceSizeCheckWithIsNotEmptyInspection() {
    val code = """
      fun foo(): Boolean {
          val arrayOf = arrayOf(1, 2, 3)
          return arrayOf.size > 0
      }
    """.trimIndent()
    val expected = """
      fun foo(): Boolean {
          val arrayOf = arrayOf(1, 2, 3)
          return arrayOf.isNotEmpty()
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceSizeZeroCheckWithIsEmptyInspection() {
    val code = """
      fun foo(): Boolean {
          val arrayOf = arrayOf(1, 2, 3)
          return arrayOf.size == 0
      }
    """.trimIndent()
    val expected = """
      fun foo(): Boolean {
          val arrayOf = arrayOf(1, 2, 3)
          return arrayOf.isEmpty()
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testSelfAssignmentInspection() {
    val code = """
      fun test() {
          var bar = 1
          bar = bar
      }
    """.trimIndent()
    val expected = """
      fun test() {
          var bar = 1
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testSimplifyBooleanWithConstantsInspection() {
    val code = """
      fun use(arg: Boolean) {
          if (false == arg) {
            val a = 0
          }
      }
    """.trimIndent()
    val expected = """
      fun use(arg: Boolean) {
          if (!arg) {
            val a = 0
          }
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  @Test
  fun testReplaceToStringWithStringTemplateInspection() {
    val code = """
      fun test(): String {
          val x = 1
          return x.toString()
      }
    """.trimIndent()
    val expected = """
      fun test(): String {
          val x = 1
          return "${'$'}x"
      }
    """.trimIndent()
    assertEquals(expected, applyInspections(code, project, language))
  }

  override fun createCourse(): Course = createKotlinCourse()
}
