package com.jetbrains.edu.coursecreator

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.toCourseInfoHolder
import org.junit.Test

class FileAttributesTest : EduTestCase() {

  @Test
  fun `attributes evaluation`() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.kt", visible = true)
          taskFile("TaskFile2.kt", visible = false)
        }
      }

      additionalFile("x.txt")
      additionalFile("y.txt")
      additionalFile("a/x.txt")
      additionalFile("a/y.txt")
      additionalFile("bb/x.txt")
      additionalFile("bb/y.txt")
      additionalFile("bb/cc/x.txt")
      additionalFile("bb/cc/y.txt")
      additionalFile("aa/bb/cc/dd/ee/x.txt")
      additionalFile("aa/bb/cc/dd/ee/y.txt")
    }

    val builder = AttributesEvaluator {
      dir("a") {
        excludeFromArchive()
      }

      dir("b.".toRegex()) {
        excludeFromArchive()
      }

      path("oo", "bb") {
        name("x.txt") {
          excludeFromArchive()
        }
      }
    }

    val holder = project.toCourseInfoHolder()
    assertTrue(builder.attributesForFile(holder, file("a/x.txt")).excludedFromArchive)
    assertTrue(builder.attributesForFile(holder, file("a/y.txt")).excludedFromArchive)
    assertTrue(builder.attributesForFile(holder, file("bb/x.txt")).excludedFromArchive)
    assertTrue(builder.attributesForFile(holder, file("bb/x.txt")).excludedFromArchive)
    assertTrue(builder.attributesForFile(holder, file("aa/bb/cc/dd/ee/x.txt")).excludedFromArchive)
    assertFalse(builder.attributesForFile(holder, file("aa/bb/cc/dd/ee/y.txt")).excludedFromArchive)
  }

  private fun file(path: String): VirtualFile =
    project.courseDir.findFileByRelativePath(path) ?: error("Can't find `$path`")

}