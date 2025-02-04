package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import org.jetbrains.kotlin.psi.KtFunction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class FunctionDataDependencyTest
  (
  private val ktFunctionName: String,
  private val expectedDependencies: Map<String, Set<String>>
) : EduTestCase() {

  private var virtualFile: VirtualFile? = null

  @Test
  fun `test data dependencies`() {
    virtualFile?.let { file ->
      val psiFile = file.findPsiFile(project)
      assertNotNull("psi File for the virtual file `$virtualFile` is null", psiFile)
      val ktFunction = PsiTreeUtil.findChildrenOfType(psiFile, KtFunction::class.java).find { it.name == ktFunctionName }
      checkNotNull(ktFunction) { "psi function with name `$ktFunctionName` hasn't been found" }
      val actual = FunctionDataDependency(ktFunction).dependenciesBackward.mapValues { (_, dependencies) ->
        dependencies.map { it.text }.toHashSet()
      }.mapKeys { it.key.text.split("\n").firstOrNull()?.trimIndent() } // to simplify expected data
      assertEquals(expectedDependencies, actual)
    }
  }

  @Before
  fun initialise() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    virtualFile = task.taskFiles.values.first().getVirtualFile(project) ?: error("Can't find virtual file for `${task.name}` task")
  }

  override fun createCourse() {
    StudyTaskManager.Companion.getInstance(project).course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile(
            name = "Main.kt",
            text = """
            object Task {
  fun simpleFunWithoutParametersTest() {
    var a = 1
    var b = 2
    b += 3
    var s = a + b
    var q = s + 3 + b
  }
  
    fun simpleFunctionWithParametersTest(a: Int) {
      var b = 2
      b += 3
      var s = a + b
      var q = s + 3 + b
    }
    
    fun simpleInlineFunction(a: Int) : Boolean = a++
    
      fun functionWhileLoopTest() {
        var n = readln().toInt()
        var i = 1
        var sum = 0
        var prod = 1
        while (i <= n) {
          sum += i
          prod *= i
          i++
        }
        println(sum)
        println(prod)
      }
}

  fun functionForLoopTest() {
    var n = readln().toInt()
    var sum = 0
    var prod = 1
    for (i in 0..n) {
      sum += i
      prod *= i
    }
    println(sum)
    println(prod)
  }
  
  fun whenTest() {
    val a = 1
    var s = 2
    when (a) {
      1 -> println(1)
      2 -> {
        s += 5
        s += a
      }
      else -> s += a + a
    }
    s += 1
  }
  
  fun ifTest() {
    var a = 1
    var b = a * 2
    if (b > 10) {
      a += 2
    }
    else {
      b += a
    }
    b += a + 1
  }
  
  fun complexTest() {
     var a = 10
    var b = a * 2
    for (q in 0..a) {
      while (b > 10) {
        b += q
      }
      a += 1
      if (b > q) {
        a += 2
      }
      else {
        b += a
      }
      when (a) {
        1 -> {
          println(a)
        }

        2 -> {
          b += 2
        }
        else -> println(b)
      }
    }
    val c = a + b
    b += a + c
  }
            """.trimIndent()
          )
        }
      }
    }
  }

  companion object {

    @Parameterized.Parameters(name = "{1}")
    @JvmStatic
    fun data(): Collection<Array<Any>> = listOf(
      arrayOf(
        "simpleFunWithoutParametersTest",
        mapOf(
          "var s = a + b" to setOf("var a = 1", "var b = 2", "b += 3"),
          "var q = s + 3 + b" to setOf("var s = a + b", "var b = 2", "b += 3"),
          "b += 3" to setOf("var b = 2")
        )
      ),
      arrayOf(
        "simpleFunctionWithParametersTest",
        mapOf(
          "var s = a + b" to setOf("a: Int", "var b = 2", "b += 3"),
          "var q = s + 3 + b" to setOf("var s = a + b", "var b = 2", "b += 3"),
          "b += 3" to setOf("var b = 2")
        )
      ),
      arrayOf(
        "simpleInlineFunction",
        mapOf("a++" to setOf("a: Int"))
      ),
      arrayOf(
        "functionWhileLoopTest",
        mapOf(
          "while (i <= n) {" to setOf("var n = readln().toInt()", "var i = 1", "i++"),
          "i++" to setOf("var i = 1"),
          "sum += i" to setOf("var sum = 0", "var i = 1", "i++"),
          "prod *= i" to setOf("var prod = 1", "var i = 1", "i++"),
          "println(sum)" to setOf("var sum = 0", "sum += i"),
          "println(prod)" to setOf("var prod = 1", "prod *= i")
        )
      ),
      arrayOf(
        "functionForLoopTest",
        mapOf(
          "for (i in 0..n) {" to setOf("var n = readln().toInt()"),
          "sum += i" to setOf("var sum = 0", "i"),
          "prod *= i" to setOf("var prod = 1", "i"),
          "println(sum)" to setOf("var sum = 0", "sum += i"),
          "println(prod)" to setOf("var prod = 1", "prod *= i")
        )
      ),
      arrayOf(
        "whenTest",
        mapOf(
          "when (a) {" to setOf("val a = 1"),
          "s += 5" to setOf("var s = 2"),
          "s += a" to setOf("val a = 1", "var s = 2", "s += 5"),
          "s += a + a" to setOf("var s = 2", "val a = 1"),
          "s += 1" to setOf("var s = 2", "s += 5", "s += a", "s += a + a")
        )
      ),
      arrayOf(
        "ifTest",
        mapOf(
          "var b = a * 2" to setOf("var a = 1"),
          "if (b > 10) {" to setOf("var b = a * 2"),
          "a += 2" to setOf("var a = 1"),
          "b += a" to setOf("var a = 1", "var b = a * 2"),
          "b += a + 1" to setOf("var a = 1", "b += a", "a += 2", "var b = a * 2")
        )
      ),
      arrayOf(
        "complexTest",
        mapOf(
          "var b = a * 2" to setOf("var a = 10"),
          "for (q in 0..a) {" to setOf("var a = 10"),
          "while (b > 10) {" to setOf("var b = a * 2", "b += q"),
          "b += q" to setOf("var b = a * 2", "q"),
          "a += 1" to setOf("var a = 10"),
          "if (b > q) {" to setOf("var b = a * 2", "b += q", "q"),
          "a += 2" to setOf("var a = 10", "a += 1"),
          "b += a" to setOf("var a = 10", "var b = a * 2", "b += q", "a += 1"),
          "when (a) {" to setOf("var a = 10", "a += 1", "a += 2"),
          "println(a)" to setOf("var a = 10", "a += 1", "a += 2"),
          "b += 2" to setOf("var b = a * 2", "b += q", "b += a"),
          "println(b)" to setOf("var b = a * 2", "b += q", "b += a"),
          "val c = a + b" to setOf("var a = 10", "var b = a * 2", "b += q", "b += a", "b += 2", "a += 1", "a += 2"),
          "b += a + c" to setOf("var a = 10", "var b = a * 2", "b += q", "b += a", "b += 2", "a += 1", "a += 2", "val c = a + b")
        )
      )
    )
  }
}
