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
class FunctionControlDependencyTest(
  private val ktFunctionName: String,
  private val expectedDependencies: Map<String, Set<Pair<String, Int>>>
) : EduTestCase() {

  private var virtualFile: VirtualFile? = null

  @Before
  fun initialise() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    virtualFile = task.taskFiles.values.first().getVirtualFile(project) ?: error("Can't find virtual file for `${task.name}` task")
  }

  @Test
  fun `test control dependencies`() {
    virtualFile?.let { file ->
      val psiFile = file.findPsiFile(project)
      assertNotNull("psi File for the virtual file `$virtualFile` is null", psiFile)
      val ktFunction = PsiTreeUtil.findChildrenOfType(psiFile, KtFunction::class.java).find { it.name == ktFunctionName }
      checkNotNull(ktFunction) { "psi function with name `$ktFunctionName` hasn't been found" }
      val actual = FunctionControlDependency(ktFunction).dependenciesForward.mapValues { (_, dependencies) ->
        dependencies.toList().map { it.toString() }
          .groupBy { it }.mapValues { (_, occurrences) -> occurrences.size }.toList().toSet()
      }.mapKeys { it.key.toString() }
      assertEquals(expectedDependencies, actual)
    }
  }

  override fun createCourse() {
    StudyTaskManager.Companion.getInstance(project).course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile(
            name = "Main.kt",
            text = """
              object Task {
    fun whenTest() {
        var sum = 0
        val a = "a"
        when (a) {
            "b" -> {
                println(1)
                println(2)
            }

            "c" -> {
                println(1)
            }

            else -> sum += 1

        }
    }

    fun noDependencyTest() {
        val a = "a"
        var i = 1
        var sum = 0
        var prod = 1
        prod += sum
        sum = 0
        println("invoke")
        return
    }

    fun singleIfTest() {
        var test = true
        if (test) {
            println(test)
            test = false
        }
        test = true
    }

    fun ifElseTest() {
        var test = true
        if (test) {
            println(test)
            test = false
        } else {
            val a = 1
        }
        test = true
    }

    fun forTest() {
        val n = readln().toInt()
        var sum = 0
        for (q in 0..n) {
            println(q)
            sum += q
        }
        println(sum)
    }


    fun whileTest() {
        val n = readln().toInt()
        var sum = 0
        while (sum < n) {
            println(sum)
            sum += 1
        }
        println(sum)
    }

    fun unreachableTest() {
        val n = readln().toInt()
        var sum = 0
        while (sum < n) {
            println(sum)
            break
            sum += 1
        }
        println(sum)
    }

    fun complexTest() {
        var sum = 0
        var prod = 1
        for (q in 0..10) {
            if (true) {
                prod = prod * 2
            } else {
                println(prod)
            }
        }
        val a = "b"
        when (a) {
            "a" -> {
                println("test")
            }

            else -> sum += 1

        }
        println(sum)
        println(prod)
    }
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
        "whenTest",
        mapOf("WHEN" to setOf("CALL_EXPRESSION" to 3, "BINARY_EXPRESSION" to 1))
      ),
      arrayOf(
        "noDependencyTest",
        emptyMap<String, Set<Pair<String, Int>>>()
      ),
      arrayOf(
        "singleIfTest",
        mapOf("IF" to setOf("CALL_EXPRESSION" to 1, "BINARY_EXPRESSION" to 1))
      ),
      arrayOf(
        "ifElseTest",
        mapOf("IF" to setOf("CALL_EXPRESSION" to 1, "BINARY_EXPRESSION" to 1, "PROPERTY" to 1))
      ),
      arrayOf(
        "forTest",
        mapOf("FOR" to setOf("CALL_EXPRESSION" to 1, "BINARY_EXPRESSION" to 1))
      ),
      arrayOf(
        "whileTest",
        mapOf("WHILE" to setOf("CALL_EXPRESSION" to 1, "BINARY_EXPRESSION" to 1))
      ),
      arrayOf(
        "unreachableTest",
        mapOf("WHILE" to setOf("CALL_EXPRESSION" to 1))
      ),
      arrayOf(
        "unreachableTest",
        mapOf("WHILE" to setOf("CALL_EXPRESSION" to 1))
      ),
      arrayOf(
        "complexTest",
        mapOf(
          "FOR" to setOf("IF" to 1),
          "IF" to setOf("BINARY_EXPRESSION" to 1, "CALL_EXPRESSION" to 1),
          "WHEN" to setOf("CALL_EXPRESSION" to 1, "BINARY_EXPRESSION" to 1)
        )
      )
    )
  }
}