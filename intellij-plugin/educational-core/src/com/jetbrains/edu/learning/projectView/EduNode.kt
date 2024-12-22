package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation
import org.jetbrains.annotations.TestOnly

abstract class EduNode<T : StudyItem>(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  protected val context: CourseViewContext,
  open val item: T?
) : PsiDirectoryNode(project, value, viewSettings) {

  init {
    myName = value.name
  }

  override fun updateImpl(data: PresentationData) {
    data.clearText()
    val item = item ?: return
    val translatedName = TranslationProjectSettings.getInstance(project).getStudyItemTranslatedName(item)
    val name = translatedName ?: item.presentableName
    val icon = CourseViewUtils.getIcon(item)
    data.addText(name, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLACK))
    additionalInfo?.let { data.addText(" $additionalInfo", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
    data.setIcon(icon)
  }

  open val additionalInfo: String?
    get() {
      val item = item ?: return null
      return if (!item.course.isStudy && item.presentableName != item.name) "(${item.name})" else null
    }

  override fun hasProblemFileBeneath(): Boolean = false

  @TestOnly
  override fun getTestPresentation(): String? = testPresentation(this)

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    return ProjectViewDirectoryHelper.getInstance(myProject)
      .getDirectoryChildren(value, settings, true, null)
      .mapNotNull { modifyChildNode(it) }
  }

  protected open fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? = childNode
}
