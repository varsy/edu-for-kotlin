package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

fun createConfigFiles(project: Project, saveTexts: Boolean = false) {
  project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, true)
  YamlFormatSynchronizer.saveAll(project, saveTexts)
  FileDocumentManager.getInstance().saveAllDocuments()
  UIUtil.dispatchAllInvocationEvents()
}
