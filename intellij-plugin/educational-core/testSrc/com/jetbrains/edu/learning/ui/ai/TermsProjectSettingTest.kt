package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import org.junit.Test

class TermsProjectSettingTest : EduSettingsServiceTestBase() {
  private val termsStorage
    get() = TermsProjectSettings.Companion.getInstance(project)

  @Test
  fun `test empty storage serialization`() {
    termsStorage.checkState("""
      <TermsProjectState />
    """.trimIndent())
  }

  @Test
  fun `test serialization in simple lesson`() = doSerializationTest(initCourse())

  @Test
  fun `test serialization in framework lesson`() = doSerializationTest(initFrameworkCourse())

  @Test
  fun `test adding term translation for another language`() {
    val course = initCourse()
    val task1 = course.findTask("lesson1", "task1").apply { id = 100 }

    with(termsStorage) {
      val termsProperties1 = TermsProperties(
        language = TranslationLanguage.ENGLISH,
        terms = mapOf(task1.id to listOf(Term("A", "B"), Term("B", "C"))),
        version = TermsVersion(1)
      )
      setTerms(termsProperties1)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>English</currentTermsLanguage>
        <terms>
          <map>
            <entry key="English">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="A" definition="B" />
                        <Term value="B" definition="C" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="English" value="1" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      val termsProperties2 = TermsProperties(
        language = TranslationLanguage.RUSSIAN,
        terms = mapOf(task1.id to listOf(Term("X", "Y"), Term("Y", "Z"))),
        version = TermsVersion(2)
      )
      setTerms(termsProperties2)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>Russian</currentTermsLanguage>
        <terms>
          <map>
            <entry key="English">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="A" definition="B" />
                        <Term value="B" definition="C" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
            <entry key="Russian">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="X" definition="Y" />
                        <Term value="Y" definition="Z" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="English" value="1" />
            <entry key="Russian" value="2" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      cleanUpState()
      checkState("""
         <TermsProjectState />
      """.trimIndent()
      )
    }
  }

  private fun doSerializationTest(course: Course) {
    val task1 = course.findTask("lesson1", "task1").apply { id = 100 }

    with(termsStorage) {
      val termsProperties1 = TermsProperties(
        language = TranslationLanguage.ENGLISH,
        terms = mapOf(task1.id to listOf(Term("A", "B"), Term("B", "C"))),
        version = TermsVersion(1)
      )
      setTerms(termsProperties1)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>English</currentTermsLanguage>
        <terms>
          <map>
            <entry key="English">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="A" definition="B" />
                        <Term value="B" definition="C" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="English" value="1" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      val termsProperties2 = TermsProperties(
        language = TranslationLanguage.ENGLISH,
        terms = mapOf(task1.id to listOf(Term("X", "Y"), Term("Y", "Z"))),
        version = TermsVersion(2)
      )
      setTerms(termsProperties2)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>English</currentTermsLanguage>
        <terms>
          <map>
            <entry key="English">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="X" definition="Y" />
                        <Term value="Y" definition="Z" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="English" value="2" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      cleanUpState()
      checkState("""
         <TermsProjectState />
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