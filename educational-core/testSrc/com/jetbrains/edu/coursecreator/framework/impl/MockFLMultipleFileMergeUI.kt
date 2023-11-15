package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.merge.MergeSession
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.framework.impl.diff.FLMergeDialogCustomizer
import com.jetbrains.edu.learning.framework.impl.diff.FLMergeProvider
import com.jetbrains.edu.learning.framework.impl.diff.FLMultipleFileMergeUI

// Always accepts changes
class MockFLMultipleFileMergeUI(
  private val resolutions: List<MergeSession.Resolution>,
  private val cancelOnStep: Int,
): FLMultipleFileMergeUI {
  private var counter = 0

  override fun show(
    project: Project,
    files: List<VirtualFile>,
    mergeProvider: FLMergeProvider,
    mergeCustomizer: FLMergeDialogCustomizer,
    currentTaskName: String,
    targetTaskName: String
  ): Boolean {
    mergeProvider.FLMergeSession().acceptFilesRevisions(files.toMutableList(), resolutions[counter])
    counter++
    return counter < cancelOnStep
  }
}