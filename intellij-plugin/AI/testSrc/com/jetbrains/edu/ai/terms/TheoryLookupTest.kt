package com.jetbrains.edu.ai.terms

import com.intellij.util.application
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.waitFor
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class TheoryLookupTest : EduTestCase() {

  @Test
  fun `test terms loading`() {
    // given
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse

    val termsLanguage = TranslationLanguage.ENGLISH
    val termsVersion = 1

    val response = CourseTermsResponse(
      TermsVersion(termsVersion),
      termsLanguage,
      mapOf(
        "3" to listOf(Term("X", "Y"), Term("Y", "X")),
        "4" to listOf(Term("A", "B"), Term("C", "D")),
      ),
    )

    val mockedService = mockService<TermsServiceConnector>(application)
    coEvery { mockedService.getCourseTerms(course.id, course.marketplaceCourseVersion, termsLanguage) } returns Ok(response)

    // when
    performAndWait(expectedVersion = termsVersion) {
      TermsLoader.getInstance(project).fetchAndApplyTerms(course, termsLanguage)
    }

    // then
    coVerify(exactly = 1) { mockedService.getCourseTerms(course.id, course.marketplaceCourseVersion, termsLanguage) }

    val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value
    val expectedTermsProperties = TermsProperties(
      language = termsLanguage,
      terms = mapOf(
        3 to listOf(Term("X", "Y"), Term("Y", "X")),
        4 to listOf(Term("A", "B"), Term("C", "D"))
      ),
      version = TermsVersion(termsVersion)
    )
    assertEquals(expectedTermsProperties, termsProperties)
  }

  private fun performAndWait(expectedVersion: Int, action: () -> Unit) {
    val updated = AtomicBoolean(false)

    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
      TermsProjectSettings.getInstance(project).termsProperties.collectLatest {
        if (it?.version?.value == expectedVersion) {
          updated.set(true)
        }
      }
    }
    try {
      action()
      waitFor { updated.get() }
    }
    finally {
      job.cancel()
    }
  }
}
