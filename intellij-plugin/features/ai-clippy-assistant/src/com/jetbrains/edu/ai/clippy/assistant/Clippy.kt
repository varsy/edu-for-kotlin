package com.jetbrains.edu.ai.clippy.assistant

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.clippy.assistant.ui.EduAiClippyImages
import com.jetbrains.edu.ai.clippy.assistant.ui.ScaledImageLabel
import java.awt.Point
import java.awt.Toolkit

class Clippy {
  private val popup = create()

  val isDisposed: Boolean
    get() = popup.isDisposed

  val isVisible: Boolean
    get() = popup.isVisible

  @RequiresEdt
  fun show(project: Project) {
    require (!popup.isDisposed) { "Clippy is disposed" }
    if (isVisible) return

    val window = WindowManager.getInstance().getIdeFrame(project) ?: error("No IDE frame found")
    val component = window.component

    val bottomRightPoint = getPoint()
    val relativePoint = RelativePoint(component, bottomRightPoint)
    popup.show(relativePoint)
  }

  private fun create(): JBPopup {
    val label = ScaledImageLabel(EduAiClippyImages.Clippy)
    val button = IconButton(
      CommonBundle.message("action.text.close"),
      AllIcons.Actions.Close,
      AllIcons.Actions.CloseHovered
    )
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(label, label)
      .setShowBorder(false)
      .setShowShadow(false)
      .setMovable(true)
      .setCancelKeyEnabled(true)
      .setCancelButton(button)
      .setCancelOnClickOutside(false)
      .setCancelOnWindowDeactivation(true)
      .setFocusable(true)
      .setRequestFocus(true)
      .createPopup()
    return popup
  }

  private fun getPoint(): Point {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val xOffset = JBUIScale.scale(300)
    val yOffset = JBUIScale.scale(300)
    return Point(screenSize.width - xOffset, screenSize.height - yOffset)
  }
}