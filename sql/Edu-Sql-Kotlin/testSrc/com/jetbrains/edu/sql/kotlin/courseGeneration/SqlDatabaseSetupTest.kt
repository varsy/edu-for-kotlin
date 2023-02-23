package com.jetbrains.edu.sql.kotlin.courseGeneration

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.getTask

class SqlDatabaseSetupTest : JvmCourseGenerationTestBase() {

  fun `test data source creation`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
        }
      }
    }

    createCourseStructure(course)

    val dataSources = LocalDataSourceManager.getInstance(project).dataSources
    val tasks = course.allTasks.toMutableSet()

    for (dataSource in dataSources) {
      val url = dataSource.url ?: error("Unexpected null url for `${dataSource.name}` data source")
      val result = DATA_SOURCE_URL_REGEX.matchEntire(url) ?: error("`$url` of `${dataSource.name}` data source doesn't match `${DATA_SOURCE_URL_REGEX.pattern}` regex")
      val taskPath = result.groups["path"]!!.value
      // It relies on fact that `CourseGenerationTestBase` is heavy test, and it uses real filesystem
      val taskDir = LocalFileSystem.getInstance().findFileByPath(taskPath) ?: error("Can't find `$taskPath`")
      val task = taskDir.getTask(project) ?: error("Can't find task for `${dataSource.name}` data source")
      tasks -= task
    }

    check(tasks.isEmpty()) {
      "Tasks ${tasks.joinToString { "`${it.presentableName}`" }} don't have data sources"
    }
  }

  fun `test attach jdbc console`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
        }
      }
    }

    createCourseStructure(course)

    val fileEditorManager = FileEditorManager.getInstance(project)
    fileEditorManager.openFiles.forEach {
      checkJdbcConsoleForFile(it)
    }
    val sqlFile = findFile("lesson1/task2/src/task.sql")
    fileEditorManager.openFile(sqlFile, true)
    checkJdbcConsoleForFile(sqlFile)
  }

  private fun checkJdbcConsoleForFile(file: VirtualFile) {
    if (file.fileType != SqlFileType.INSTANCE) return
    // `SqlGradleStartupActivity` attaches console using `invokeLater` so we have to dispatch events in EDT
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    val console = JdbcConsoleProvider.getValidConsole(project, file)
    assertNotNull("Can't find jdbc console for `$file`", console)
  }

  companion object {
    /**
     * Heavily depends on [com.jetbrains.edu.sql.jvm.gradle.SqlGradleStartupActivity.databaseUrl]
     */
    private val DATA_SOURCE_URL_REGEX = "jdbc:h2:file:(?<path>.*)/db".toRegex()
  }
}
