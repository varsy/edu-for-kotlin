package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.IconMapLoader
import com.intellij.ide.ui.IconMapperBean
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.icons.CachedImageIcon
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Modifier
import javax.swing.Icon

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class EducationalCoreIconsTest(
  private val icon: CachedImageIcon,
  @Suppress("unused") private val name: String
) : EduTestCase() {
  @Test
  fun `test icon path is correct`() {
    val mappings = getMappings(project)

    val iconPath = icon.originalPath ?: error("Can't get original path of icon")
    doTest(iconPath)

    if (icon in excludedFromMappings) return

    val expUIIconPath = mappings[iconPath.trimStart('/')]
    Assert.assertNotNull("Path for icon ($iconPath) in new UI is null", expUIIconPath)

    if (expUIIconPath != null) {
      doTest(expUIIconPath)
    }
  }

  private fun doTest(path: String) {
    if (!path.endsWith(".svg") && !path.endsWith(".png")) {
      error("Path should end with .svg or .png : $path")
    }

    val iconUrl = (IconLoader.getIcon(path, aClass) as CachedImageIcon).url
    Assert.assertNotNull("$path doesn't exist in resources folder", iconUrl)
  }

  private fun getMappings(project: Project): Map<String, String> {
    val pluginClassLoader = IconMapperBean.EP_NAME.filterableLazySequence()
      .first { it.pluginDescriptor.name == "com.jetbrains.edu.core" }
      .pluginDescriptor
      .pluginClassLoader

    @Suppress("SSBasedInspection")
    val mappings = runBlocking { project.service<IconMapLoader>().doLoadIconMapping() }[pluginClassLoader]
                   ?: error("Unexpected amount of icon class loaders")
    return mappings
  }

  companion object {
    private val aClass = EducationalCoreIcons::class.java

    @JvmStatic
    @Parameterized.Parameters(name = "{1}")
    fun icons(): Collection<Array<Any>> = getIconPathsFromClass(aClass).map { arrayOf(it.icon, it.name) }

    /**
     * List of icons that should be ignored during the check for mapping to the new UI icon,
     * because they have already been created for the new UI
     */
    private val excludedFromMappings = listOf(
      EducationalCoreIcons.Actions.IgnoreSyncFile,
      EducationalCoreIcons.Actions.SyncChanges,
      EducationalCoreIcons.CourseCreator.GuidedProject,
      EducationalCoreIcons.CourseCreator.GuidedProjectSelected,
      EducationalCoreIcons.CourseCreator.SimpleLesson,
      EducationalCoreIcons.CourseCreator.SimpleLessonSelected,
      EducationalCoreIcons.CourseView.SyncFilesModInfo,
      EducationalCoreIcons.CourseView.SyncFilesModWarning,
      EducationalCoreIcons.Language.CSharp,
      EducationalCoreIcons.Submission.TaskFailed,
      EducationalCoreIcons.Submission.TaskFailedHighContrast,
      EducationalCoreIcons.Submission.TaskSolved,
      EducationalCoreIcons.Submission.TaskSolvedHighContrast,
      EducationalCoreIcons.DOT,
      EducationalCoreIcons.AIDebugger.BREAKPOINT,
      EducationalCoreIcons.CourseCreator.NewTask,
      EducationalCoreIcons.CourseCreator.NewLesson,
      EducationalCoreIcons.AIDebugger.BREAKPOINT
    )

    private fun getIconPathsFromClass(aClass: Class<*>): Collection<EduIconTestData> {
      val result = mutableListOf<EduIconTestData>()

      aClass.declaredFields
        .filter { field ->
          Icon::class.java.isAssignableFrom(field.type) && Modifier.isStatic(field.modifiers) && Modifier.isFinal(field.modifiers)
        }
        .forEach { field ->
          val icon = field.apply { isAccessible = true }.get(null) as? CachedImageIcon
          if (icon != null) {
            result.add(EduIconTestData(icon, field.name))
          }
        }

      aClass.declaredClasses.forEach { innerClass ->
        result.addAll(getIconPathsFromClass(innerClass))
      }

      return result
    }

    data class EduIconTestData(
      val icon: CachedImageIcon,
      val name: String
    )
  }
}