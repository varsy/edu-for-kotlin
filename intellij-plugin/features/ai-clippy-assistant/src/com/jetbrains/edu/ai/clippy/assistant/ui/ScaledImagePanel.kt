package com.jetbrains.edu.ai.clippy.assistant.ui

import com.intellij.util.IconUtil
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JLabel

class ScaledImageLabel(private val icon: Icon) : JLabel() {
  override fun getPreferredSize() = Dimension(200, 200)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val image = IconUtil.toImage(icon)

    val imageWidth = image.getWidth(this)
    val imageHeight = image.getHeight(this)

    if (imageWidth <= 0 || imageHeight <= 0) return

    val imageAspect = imageWidth.toDouble() / imageHeight

    val panelAspect = width.toDouble() / height
    val drawWidth: Int
    val drawHeight: Int

    if (imageAspect > panelAspect) {
      drawWidth = width
      drawHeight = (width / imageAspect).toInt()
    }
    else {
      drawHeight = height
      drawWidth = (height * imageAspect).toInt()
    }

    val x = (width - drawWidth) / 2
    val y = (height - drawHeight) / 2

    g.drawImage(image, x, y, drawWidth, drawHeight, this)
  }
}