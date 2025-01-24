package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.disambiguateContents
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.storage.persistAdditionalFiles
import com.jetbrains.edu.learning.storage.persistEduFiles
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.remoteMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.studentMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.studentMapperWithEncryption
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")
  val SAVE_TO_CONFIG = Key<Boolean>("Edu.saveItem")

  fun saveAll(project: Project) {
    @NonNls
    val errorMessageToLog = "Attempt to create config files for project without course"
    val course = StudyTaskManager.getInstance(project).course ?: error(errorMessageToLog)
    val mapper = course.mapper()
    saveItem(course, mapper)
    course.visitSections { section -> saveItem(section, mapper) }
    course.visitLessons { lesson ->
      lesson.visitTasks { task ->
        saveItem(task, mapper)
      }
      saveItem(lesson, mapper)
    }

    saveRemoteInfo(course)
  }

  fun saveItem(item: StudyItem, mapper: ObjectMapper = item.course.mapper(), configName: String = item.configFileName) {
    val course = item.course

    @NonNls
    val errorMessageToLog = "Failed to find project for course"
    val project = course.project ?: error(errorMessageToLog)
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return
    }
    item.saveConfig(project, configName, mapper)
  }

  fun saveRemoteInfo(item: StudyItem) {
    when (item) {
      is ItemContainer -> {
        saveItemRemoteInfo(item)
        item.items.forEach { saveRemoteInfo(it) }
      }
      is Task -> {
        saveItemRemoteInfo(item)
      }
    }
  }

  fun saveItemWithRemoteInfo(item: StudyItem) {
    saveItem(item)
    saveRemoteInfo(item)
  }

  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (item.id > 0 || item is HyperskillCourse) {
      saveItem(item, remoteMapper(), item.remoteConfigFileName)
    }
  }

  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }

    val disposable = StudyTaskManager.getInstance(project)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), disposable)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          if (project.isStudentProject()) {
            @NonNls
            val errorMessageToLog = "Can't find editor for a file: ${file.name}"
            val editor = file.getEditor(project) ?: error(errorMessageToLog)
            showNoEditingNotification(editor)
            return
          }

          // load item to show editor notification if config file is invalid
          YamlLoader.loadItem(project, file, false)
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel(EduCoreBundle.message("yaml.editor.notification.configuration.file"))
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  private fun StudyItem.saveConfig(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getConfigDir(project)

    val configFile = runReadAction { dir.findChild(configName) }
    if (configFile?.getUserData(SAVE_TO_CONFIG) == false) return

    if (this is Task) {
      disambiguateTaskFilesContents(project)
      persistEduFiles(project)
    }

    if (this is Course) {
      disambiguateAdditionalFilesContents(project)
      persistAdditionalFiles(project)
    }

    project.invokeLater {
      runWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)
        try {
          file.putUserData(LOAD_FROM_CONFIG, false)
          if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
            @NonNls
            val errorMessageToLog = "Failed to get extension for file ${file.name}"
            FileTypeManager.getInstance().associateExtension(
              PlainTextFileType.INSTANCE,
              file.extension ?: error(errorMessageToLog)
            )
          }
          val yamlText = mapper.writeValueAsString(this)
          val formattedYamlText = reformatYaml(project, file.name, yamlText)

          VfsUtil.saveText(file, formattedYamlText)
          // make sure that there is no conflict between disk contents and ide in-memory document contents
          FileDocumentManager.getInstance().reloadFiles(file)
        }
        finally {
          file.putUserData(LOAD_FROM_CONFIG, true)
        }
      }
    }
  }

  private fun reformatYaml(project: Project, fileName: String, text: String): String {
    // We are able to reformat YAML only if the IDE supports the YAML language
    val yamlFileType = FileTypeManager.getInstance().findFileTypeByName("YAML") ?: return text

    val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, yamlFileType, text)
    CodeStyleManager.getInstance(project).reformat(psiFile)

    return psiFile.text ?: text
  }

  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isRemoteConfigFileName(name)
  }

  fun isRemoteConfigFileName(name: String): Boolean {
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isLocalConfigFileName(name)
  }

  fun isLocalConfigFileName(name: String): Boolean {
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  fun Course.mapper(): ObjectMapper = if (isStudy) {
    if (isMarketplace) studentMapperWithEncryption() else studentMapper()
  }
  else {
    basicMapper()
  }
}

fun StudyItem.getConfigDir(project: Project): VirtualFile {
  return if (this is Task && lesson is FrameworkLesson) {
    @NonNls
    val errorMessageToLog = "Config for '$name' task dir in guided project not found"
    lesson.getDir(project.courseDir)?.findChild(name) ?: error(errorMessageToLog)
  }
  else {
    getDir(project.courseDir) ?: error("Config for '$this' not found")
  }
}

private fun Task.disambiguateTaskFilesContents(project: Project) {
  for ((path, taskFile) in taskFiles) {
    val file = taskFile.getVirtualFile(project)
    val disambiguatedContents = if (file != null) {
      taskFile.contents.disambiguateContents(file)
    }
    else {
      taskFile.contents.disambiguateContents(path)
    }

    taskFile.contents = disambiguatedContents
  }
}

private fun Course.disambiguateAdditionalFilesContents(project: Project) {
  val courseDir = project.courseDir

  additionalFiles.map { additionalFile ->
    val filePath = additionalFile.name
    val file = courseDir.findFile(filePath)

    val disambiguatedContents = if (file != null) {
      additionalFile.contents.disambiguateContents(file)
    }
    else {
      additionalFile.contents.disambiguateContents(filePath)
    }

    additionalFile.contents = disambiguatedContents
  }
}