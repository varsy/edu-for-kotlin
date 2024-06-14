package com.jetbrains.edu.rust

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import org.rust.cargo.project.RsToolchainPathChoosingComboBox
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.nio.file.Path

fun RsToolchainPathChoosingComboBox(
  parentDisposable: Disposable,
  onTextChanged: () -> Unit
): RsToolchainPathChoosingComboBox = RsToolchainPathChoosingComboBox(parentDisposable.disposingScope(), onTextChanged)

fun RsToolchainPathChoosingComboBox.addToolchainsAsync(
  toolchainObtainer: () -> List<Path>,
  onFinish: () -> Unit
) {
  // The following listener assumes implicitly that it's impossible to select something before `addToolchainsAsync` finishes.
  // But it still looks like the best way to handle absence of callback in `addToolchainsAsync` since `241.27011`
  childComponent.addItemListener(object : ItemListener {
    override fun itemStateChanged(e: ItemEvent) {
      if (e.stateChange == ItemEvent.SELECTED) {
        childComponent.removeItemListener(this)
        // `invokeLater` is important here since listener is invoked
        // before `RsToolchainPathChoosingComboBox` finishes all changes after toolchain loading.
        // Otherwise, our changes may have no effect
        invokeLater {
          onFinish()
        }
      }
    }
  })

  addToolchainsAsync(toolchainObtainer)
}
