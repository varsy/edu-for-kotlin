package com.jetbrains.edu.theoryLookup

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.theoryLookup.TheoryLookupTermsManager
import com.jetbrains.educational.ml.theory.lookup.term.Term
import org.junit.Test

class TheoryLookupTermsSerializationTest : EduSettingsServiceTestBase() {
  private val termsStorage
    get() = TheoryLookupTermsManager.getInstance(project)

  @Test
  fun `test empty storage serialization`() {
    termsStorage.checkState("""
      <TermsState />
    """.trimIndent())
  }

  @Test
  fun `test storage serialization in simple lesson`() = doTest(initCourse())

  @Test
  fun `test storage serialization in framework lesson`() = doTest(initFrameworkCourse())

  private fun doTest(course: Course) {
    val task1 = course.findTask("lesson1", "task1").apply { id = 100 }

    with(termsStorage) {
      setTaskTerms(task1, listOf(Term("A", "B"), Term("B", "C")))
      checkState("""
        <TermsState>
          <taskTerms>
            <map>
              <entry key="100">
                <value>
                  <map>
                    <entry key="A" value="B" />
                    <entry key="B" value="C" />
                  </map>
                </value>
              </entry>
            </map>
          </taskTerms>
        </TermsState>
      """.trimIndent())

      setTaskTerms(task1, listOf(Term("X", "Y"), Term("Y", "Z")))
      checkState("""
        <TermsState>
          <taskTerms>
            <map>
              <entry key="100">
                <value>
                  <map>
                    <entry key="X" value="Y" />
                    <entry key="Y" value="Z" />
                  </map>
                </value>
              </entry>
            </map>
          </taskTerms>
        </TermsState>
      """.trimIndent())

      println(state.modificationCount)

      cleanUpState()
      checkState("""
         <TermsState />
      """.trimIndent()
      )
    }
  }

  private fun initCourse(): Course = courseWithFiles(
    courseMode = CourseMode.STUDENT,
    language = FakeGradleBasedLanguage,
  ) {
    lesson("lesson1") {
      eduTask("task1") {
        taskFile("Task.kt", "fun foo()")
      }
    }
  }

  private fun initFrameworkCourse(): Course = courseWithFiles(
    courseMode = CourseMode.STUDENT,
    language = FakeGradleBasedLanguage,
  ) {
    frameworkLesson("lesson1") {
      eduTask("task1") {
        taskFile("Task.kt", "fun foo()")
      }
    }
  }
}