/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.edu.learning.projectView

import com.intellij.ide.SelectInTarget
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons.CourseView.CourseTree
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.projectView.*
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.ProgressUtil.createProgressBar
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer

class CourseViewPane(project: Project) : AbstractProjectViewPaneWithAsyncSupport(project) {

  private val myStudyItemDeleteProvider = CCStudyItemDeleteProvider()

  private lateinit var progressBar: JProgressBar

  private val courseViewComponent: JComponent by lazy { createCourseViewComponent() }

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    return object : ProjectViewTree(treeModel) {
      override fun toString(): String = "$title ${super.toString()}"

      override fun createCellRenderer(): TreeCellRenderer {
        val projectViewRenderer = super.createCellRenderer()
        if (CCUtils.isCourseCreator(myProject)) {
          return CCCellRenderer(projectViewRenderer as ColoredTreeCellRenderer)
        }
        return projectViewRenderer
      }
    }
  }

  override fun createComponent(): JComponent = courseViewComponent

  override fun createComparator(): Comparator<NodeDescriptor<*>> = EduNodeComparator

  private fun createCourseViewComponent(): JComponent {
    if (!userAgreementSettings().isPluginAllowed) {
      return super.createComponent()
    }
    super.createComponent()
    CourseViewPaneCustomization.customize(tree)
    if (!myProject.isStudentProject()) {
      HelpTooltipForTree().installOnTree(this, tree) { treeNode ->
        tryInstallNewTooltip(myProject, treeNode)
      }

      val toolbar = createHeaderRightToolbar()
      val panel = panel {
        row {
          cell(EducatorActionsPanel())
          cell(toolbar.component).align(AlignX.RIGHT)
        }
      }.apply {
        border = JBUI.Borders.emptyRight(12)
      }
      toolbar.targetComponent = panel

      val mainPanel = JPanel(BorderLayout())
      mainPanel.background = UIUtil.getTreeBackground()

      mainPanel.add(panel, BorderLayout.NORTH)
      mainPanel.add(tree, BorderLayout.CENTER)

      return ScrollPaneFactory.createScrollPane(mainPanel)
    }

    val panel = JPanel(BorderLayout())
    panel.background = UIUtil.getTreeBackground()

    panel.add(createProgressPanel(), BorderLayout.NORTH)
    panel.add(tree, BorderLayout.CENTER)

    if (StudyTaskManager.getInstance(myProject).course != null) {
      updateCourseProgress()
    }
    else {
      val connection = myProject.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          updateCourseProgress()
        }
      })
    }
    return ScrollPaneFactory.createScrollPane(panel)
  }

  private fun createProgressPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    progressBar = createProgressBar()
    panel.background = UIUtil.getTreeBackground()
    panel.add(progressBar, BorderLayout.NORTH)
    panel.border = JBUI.Borders.emptyBottom(5)
    return panel
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup) {
    actionGroup.removeAll()
    val group = ActionManager.getInstance().getAction("Educational.CourseView.SecondaryActions") as DefaultActionGroup
    for (action in group.childActionsOrStubs) {
      actionGroup.addAction(action).setAsSecondary(true)
    }
  }

  private fun createHeaderRightToolbar(): ActionToolbar {
    val group = ActionManager.getInstance().getAction("Educational.CourseView.Header.Right") as ActionGroup
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.PROJECT_VIEW_TOOLBAR, group, true)
  }

  private fun updateCourseProgress() {
    val course = StudyTaskManager.getInstance(myProject).course
    if (course == null) {
      Logger.getInstance(CourseViewPane::class.java).error("course is null")
      return
    }
    updateCourseProgress(ProgressUtil.countProgress(course))
  }

  fun updateCourseProgress(progress: ProgressUtil.CourseProgress) {
    progressBar.maximum = progress.tasksTotalNum
    progressBar.value = progress.tasksSolved
  }

  @TestOnly
  fun getProgressBar(): JProgressBar = progressBar

  override fun createStructure(): ProjectAbstractTreeStructureBase = object : ProjectTreeStructure(myProject, ID), ProjectViewSettings {
    override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> {
      return RootNode(myProject, settings)
    }

    override fun getChildElements(element: Any): Array<Any> {
      if (element !is AbstractTreeNode<*>) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY
      }
      val elements = element.children
      elements.forEach { node -> node.setParent(element) }
      return ArrayUtil.toObjectArray(elements)
    }

    override fun isShowExcludedFiles() = false
  }

  override fun getTitle(): String = EduCoreBundle.message("project.view.course.pane.title")

  override fun getIcon(): Icon = CourseTree
  override fun getId(): String = ID
  override fun getWeight(): Int = 10

  override fun createSelectInTarget(): SelectInTarget {
    return object : ProjectViewSelectInTarget(myProject) {
      override fun getMinorViewId(): String = ID
      override fun toString(): String = ID
    }
  }

  @Suppress("UnstableApiUsage")
  override fun supportsFoldersAlwaysOnTop(): Boolean = false
  @Suppress("UnstableApiUsage")
  override fun supportsSortByType(): Boolean = false

  override fun getData(dataId: String): Any? {
    if (myProject.isDisposed) return null

    if (CCUtils.isCourseCreator(myProject)) {
      val studyItem = when (val userObject = (selectedPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject) {
        is CCTaskNode -> userObject.item
        is CCLessonNode -> userObject.item
        is CCSectionNode -> userObject.item
        else -> null
      }
      if (studyItem != null) {
        when {
          PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> return myStudyItemDeleteProvider
          STUDY_ITEM.`is`(dataId) -> return studyItem
        }
      }
    }
    return super.getData(dataId)
  }

  override fun isDefaultPane(project: Project): Boolean = project.isEduProject()

  companion object {
    @NonNls
    const val ID = "Course"
    const val HIDE_SOLVED_LESSONS = "Edu.HideSolvedLessons"

    val STUDY_ITEM: DataKey<StudyItem> = DataKey.create("Edu.studyItem")
  }

  class HideSolvedLessonsAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean {
      return PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      PropertiesComponent.getInstance().setValue(HIDE_SOLVED_LESSONS, state)
      val project = e.project ?: return
      ProjectView.getInstance(project).refresh()
    }
  }
}
