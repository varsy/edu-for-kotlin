package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.taskToolWindow.ui.JsEventData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class JsEventDataTest(private val jsEventDataJson: String) : EduTestCase() {

  @Test
  fun `test JsEventData deserialization`() {
    val jsEventData = JsEventData.fromJson(jsEventDataJson)
    assertNotNull(jsEventData)
    jsEventData!!
    assertEquals(jsEventData.term, "abc")
    assertEquals(jsEventData.x, 1)
    assertEquals(jsEventData.y, 2)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun testInfos(): Collection<Any> = listOf(
      arrayOf("""{"term":"abc","x":1.23,"y":2.34}"""),
      arrayOf("""{"term":"abc","x":1.53,"y":2.68}"""),
      arrayOf("""{"x":1.53, "term":"abc", "y":2.64}"""),
      arrayOf("""
        {
          "x":1.23,
          "y":2.64,
          "term":"abc"
        }
      """.trimIndent()),
    )
  }
}