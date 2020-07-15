package com.jetbrains.edu.rust.actions

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.rust.RsProjectSettings
import org.intellij.lang.annotations.Language
import org.rust.cargo.CargoConstants
import org.rust.lang.RsLanguage

class RsCreateLessonTest : RsActionTestBase() {

  fun `test add lesson item no trailing comma`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/"
    ]

    exclude = [
        "**/*.yaml"
    ]    
  """, """
    [workspace]
    
    members = [
        "lesson1/*/",
    "lesson2/*/",
    ]

    exclude = [
        "**/*.yaml"
    ]    
  """)

  fun `test add lesson item trailing comma`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/",
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/",
    "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  fun `test add lesson comments`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/" # very useful comment
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/", # very useful comment
    "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  fun `test add lesson extra spaces`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/" ,
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/" ,
    "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  fun `test add lesson item with section`() {
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      section("section1") {
        lesson("lesson1")
      }
      additionalFile(CargoConstants.MANIFEST_FILE, """
        [workspace]

        members = [
            "section1/lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(dataContext(findFile("section1")), CCCreateLesson())
    }

    checkCargoToml("""
      [workspace]
      
      members = [
          "section1/lesson1/*/",
      "section1/lesson2/*/",
      ]
      
      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  fun `test add the first lesson in course`() {
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      section("section1")
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson1")) {
      testAction(dataContext(findFile("section1")), CCCreateLesson())
    }

    checkCargoToml("""
      [workspace]
      
      members = [
      "section1/lesson1/*/",
      ]
      
      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  fun `test add lesson in the middle of course`() {
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      lesson("lesson1")
      lesson("lesson3")
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
            "lesson1/*/",
            "lesson3/*/"
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(dataContext(findFile("lesson1")), CCCreateLesson())
    }

    checkCargoToml("""
      [workspace]

      members = [
          "lesson1/*/",
          "lesson2/*/",
      "lesson3/*/"
      ]

      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  private fun addLastLesson(@Language("TOML") before: String, @Language("TOML") after: String) {
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      lesson("lesson1")
      additionalFile(CargoConstants.MANIFEST_FILE, before.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(dataContext(LightPlatformTestCase.getSourceRoot()), CCCreateLesson())
    }

    checkCargoToml(after)
  }
}
