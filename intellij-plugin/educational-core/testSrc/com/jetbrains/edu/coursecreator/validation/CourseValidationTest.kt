package com.jetbrains.edu.coursecreator.validation

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlFile
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection

class CourseValidationTest : EduTestCase() {

  override fun runInDispatchThread(): Boolean = false

  override fun tearDown() {
    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      super.tearDown()
    }
  }

  @Test
  fun `test events for check action`() {
    val course = courseWithFiles {
      lesson("lesson with unchecked tasks") {
        theoryTask("theory unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
        eduTask("edu unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
      }
      lesson("lesson with solved tasks") {
        theoryTask("theory solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask("edu solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        outputTask("output solved task") {
          checkResultFile("Answer")
          dir("tests") {
            taskFile("output.txt") {
              withText("Answer")
            }
          }
        }
      }
      section("section with failed tasks") {
        lesson("lesson with failed tasks") {
          theoryTask("theory failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          eduTask("edu failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          outputTask("output failed task") {
            checkResultFile("Answer1")
            dir("tests") {
              taskFile("output.txt") {
                withText("Answer2")
              }
            }
          }
        }
      }
    }

    doTest(course, validateTests = true, validateLinks = false, """
      - Test Course:
        - lesson with unchecked tasks:
          - theory unchecked task:
            - Tests: ignored
          - edu unchecked task:
            - Tests: ignored
        - lesson with solved tasks:
          - theory solved task:
            - Tests: ignored
          - edu solved task:
            - Tests: success
          - output solved task:
            - Tests: success
        - section with failed tasks:
          - lesson with failed tasks:
            - theory failed task:
              - Tests: ignored
            - edu failed task:
              - Tests: failed
            - output failed task:
              - Tests: failed
    """)
  }

  @Test
  fun `test link validation`() {
    val helper = MockWebServerHelper(testRootDisposable)

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("valid links", taskDescription = """
          [http link](${helper.baseUrl}valid)
          [file link](file://foo/bar.txt)
          [course link](course://lesson1/invalid%20links/Task2.txt)
          [psi element link](psi_element://foo.bar)
          [tool window link](tool_window://Task)
          [settings link](settings://Educational)
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task1.txt")
        }
        eduTask("invalid links", taskDescription = """
          [http link](${helper.baseUrl}invalid)
          [file link](file://foo/baz.txt)
          [course link](course://lesson1/valid%20links/Task3.txt)
          [psi element link](psi_element://foo.baz)
          [tool window link](tool_window://Task1)
          [settings link](settings://Educational2)
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task2.txt")
        }
      }
      additionalFile("foo/bar.txt")
      // Used for psi links
      additionalFile("config.xml", """<foo><bar></bar></foo>""")
    }

    // Make psi link resolution works for JSON.
    QualifiedNameProvider.EP_NAME.point.registerExtension(TestXmlQualifiedNameProvider(), testRootDisposable)
    registerTaskDescriptionToolWindow()
    helper.addResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/invalid" -> MockResponseFactory.notFound()
        "/valid" -> MockResponseFactory.ok()
        else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
      }
    }

    doTest(course, validateTests = false, validateLinks = true, """
      - Test Course:
        - lesson1:
          - valid links:
            - Task description links:
              - ${helper.baseUrl}valid: success
              - file://foo/bar.txt: success
              - course://lesson1/invalid%20links/Task2.txt: success
              - psi_element://foo.bar: success
              - tool_window://Task: success
              - settings://Educational: success
          - invalid links:
            - Task description links:
              - ${helper.baseUrl}invalid: failed
              - file://foo/baz.txt: failed
              - course://lesson1/valid%20links/Task3.txt: failed
              - psi_element://foo.baz: failed
              - tool_window://Task1: failed
              - settings://Educational2: failed
    """)
  }

  @Test
  fun `test image link validation`() {
    val helper = MockWebServerHelper(testRootDisposable)

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("valid links", taskDescription = """
          ![img file link](images/md-image.png)
          ![img http link](${helper.baseUrl}valid-md-image.png)
          <img src="images/image-1.png" srcset="images/image-srcset.png"/>
          <img src="images/image-2.png" data-dark-src="images/image-dark-src.png"/>
          <img src="${helper.baseUrl}valid-src-img-1.png" srcset="${helper.baseUrl}valid-srcset-img.png"/>
          <img src="${helper.baseUrl}valid-src-img-2.png" data-dark-src="${helper.baseUrl}valid-dark-src-img.png"/>
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task1.txt")
          taskFile("images/md-image.png")
          taskFile("images/image-1.png")
          taskFile("images/image-2.png")
          taskFile("images/image-srcset.png")
          taskFile("images/image-dark-src.png")

          dir("images") {
            // Needed to verify that we don't fail on unexpected files during calculation if a dark image path
            dir("md-image_dark.png") {
              taskFile("foo.txt")
            }
          }
        }
        eduTask("invalid links", taskDescription = """
          ![img file link](images/md-image.png)
          ![img http link](${helper.baseUrl}invalid-md-image.png)
          <img src="images/image-1.png" srcset="images/image-srcset.png"/>
          <img src="images/image-2.png" data-dark-src="images/image-dark-src.png"/>
          <img src="${helper.baseUrl}invalid-src-img-1.png" srcset="${helper.baseUrl}invalid-srcset-img.png"/>
          <img src="${helper.baseUrl}invalid-src-img-2.png" data-dark-src="${helper.baseUrl}invalid-dark-src-img.png"/>
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task2.txt")
        }
      }
    }

    registerTaskDescriptionToolWindow()
    helper.addResponseHandler(testRootDisposable) { _, path ->
      when {
        path.startsWith("/invalid") -> MockResponseFactory.notFound()
        path.startsWith("/valid") -> MockResponseFactory.ok()
        else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
      }
    }

    doTest(course, validateTests = false, validateLinks = true, """
      - Test Course:
        - lesson1:
          - valid links:
            - Task description links:
              - images/md-image.png: success
              - ${helper.baseUrl}valid-md-image.png: success
              - images/image-1.png: success
              - images/image-srcset.png: success
              - images/image-2.png: success
              - images/image-dark-src.png: success
              - ${helper.baseUrl}valid-src-img-1.png: success
              - ${helper.baseUrl}valid-srcset-img.png: success
              - ${helper.baseUrl}valid-src-img-2.png: success
              - ${helper.baseUrl}valid-dark-src-img.png: success
          - invalid links:
            - Task description links:
              - images/md-image.png: failed
              - ${helper.baseUrl}invalid-md-image.png: failed
              - images/image-1.png: failed
              - images/image-srcset.png: failed
              - images/image-2.png: failed
              - images/image-dark-src.png: failed
              - ${helper.baseUrl}invalid-src-img-1.png: failed
              - ${helper.baseUrl}invalid-srcset-img.png: failed
              - ${helper.baseUrl}invalid-src-img-2.png: failed
              - ${helper.baseUrl}invalid-dark-src-img.png: failed
    """)
  }

  private fun doTest(course: Course, validateTests: Boolean, validateLinks: Boolean, expected: String) {
    waitUntilIndexesAreReady(project)
    val testMessageConsumer = TestServiceMessageConsumer()
    val params = ValidationParams(validateTests = validateTests, validateLinks = validateLinks)
    val validationHelper = CourseValidationHelper(params, testMessageConsumer)

    runBlocking {
      validationHelper.validate(project, course)
    }

    testMessageConsumer.assertTestTreeEquals(expected)
  }
}


private class TestXmlQualifiedNameProvider : QualifiedNameProvider {
  override fun adjustElementToCopy(element: PsiElement): PsiElement? = null
  override fun getQualifiedName(element: PsiElement): String? = null

  // It shouldn't be too precise, but it should be enough to check how we work with psi links
  override fun qualifiedNameToElement(fqn: String, project: Project): PsiElement? {
    val file = project.courseDir.findFile("config.xml")?.findPsiFile(project) as? XmlFile ?: return null
    val segments = fqn.split(".")
    var currentTag = file.rootTag?.takeIf { it.name == segments[0] } ?: return null
    for (segment in segments.drop(1)) {
      currentTag = currentTag.findFirstSubTag(segment) ?: return null
    }
    return currentTag
  }
}
