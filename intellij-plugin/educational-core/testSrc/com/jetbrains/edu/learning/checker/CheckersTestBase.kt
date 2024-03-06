package com.jetbrains.edu.learning.checker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getActionById
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.junit.ComparisonFailure
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class CheckersTestBase<Settings : EduProjectSettings> : CheckersTestCommonBase<Settings>() {

    protected open fun doTest() {
        refreshProject()
        UIUtil.dispatchAllInvocationEvents()

        val exceptions = arrayListOf<AssertionError>()
        myCourse.visitLessons { lesson ->
            for (task in lesson.taskList) {
                exceptions += checkTask(task)
            }
        }

        if (exceptions.isNotEmpty()) {
            throw MultipleCauseException(exceptions)
        }
    }

    private fun refreshProject() {
        myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    protected open fun checkTask(task: Task): List<AssertionError> {
        UIUtil.dispatchAllInvocationEvents()
        val exceptions = mutableListOf<AssertionError>()
        try {
            // In case of framework lessons, we can't just run checker for the given task
            // because code on file system may be not updated yet.
            // Launch `NextTaskAction` to make all necessary updates on file system in similar was as users do it
            val frameworkLesson = task.lesson as? FrameworkLesson
            val currentFrameworkLessonTask = frameworkLesson?.currentTask()
            if (frameworkLesson != null && currentFrameworkLessonTask != null && currentFrameworkLessonTask != task) {
                val file = currentFrameworkLessonTask.openFirstTaskFileInEditor()
                launchAction(file, getActionById(NextTaskAction.ACTION_ID))
                assertEquals(task, frameworkLesson.currentTask())
            }

            val virtualFile = task.openFirstTaskFileInEditor()
            launchAction(virtualFile, CheckAction(task.getUICheckLabel()))
            UIUtil.dispatchAllInvocationEvents()
        } catch (e: AssertionError) {
            exceptions.add(e)
        } catch (e: ComparisonFailure) {
            exceptions.add(e)
        } catch (e: IllegalStateException) {
            exceptions.add(AssertionError(e))
        }
        return exceptions
    }

    private fun Task.openFirstTaskFileInEditor(): VirtualFile {
        val taskFile = taskFiles.values.first()
        val virtualFile = taskFile.getVirtualFile(myProject)
                          ?: error("Can't find virtual file for `${taskFile.name}` task file in `$name task`")

        FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
        return virtualFile
    }

    private class MultipleCauseException(val causes: List<AssertionError>) : Exception() {
        override fun printStackTrace() {
            for (cause in causes) {
                cause.printStackTrace()
            }
        }

        override val message: String
            get() = "\n" + causes.joinToString("\n") { it.message ?: "" }
    }

    private fun launchAction(virtualFile: VirtualFile, action: AnAction) {
        val context = createDataEvent(virtualFile)
        testAction(action, context)
    }

    private fun createDataEvent(virtualFile: VirtualFile): DataContext {
        return SimpleDataContext.builder()
          .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
          .add(CommonDataKeys.PROJECT, myProject)
          .build()
    }
}
