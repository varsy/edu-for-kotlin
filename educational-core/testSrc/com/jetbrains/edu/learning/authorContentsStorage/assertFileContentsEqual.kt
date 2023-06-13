package com.jetbrains.edu.learning.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import org.junit.Assert
import kotlin.test.junit.JUnitAsserter

fun assertFileContentsEqual(expected: FileContents?, actual: FileContents?) {
  if (expected == null) {
    if (actual != null) {
      JUnitAsserter.fail("file contents is expected to be null")
    }
    return
  }
  actual ?: JUnitAsserter.fail("file contents is expected to be not null")

  Assert.assertEquals("file contents has wrong binarity", expected.isBinary, actual.isBinary)

  if (expected is TextualContents) {
    actual as? TextualContents ?: JUnitAsserter.fail("file contents is expected to be textual")
    Assert.assertEquals("file contents has wrong text", expected.text, actual.text)
  }

  if (expected is BinaryContents) {
    actual as? BinaryContents ?: JUnitAsserter.fail("file contents is expected to be binary")
    Assert.assertArrayEquals(expected.bytes, actual.bytes)
  }
}