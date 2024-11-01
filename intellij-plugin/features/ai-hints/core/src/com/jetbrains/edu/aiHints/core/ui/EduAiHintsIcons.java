package com.jetbrains.edu.aiHints.core.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@SuppressWarnings("unused")
public final class EduAiHintsIcons {
  public static final Icon Hint = load("/icons/hint.svg");

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@SuppressWarnings("SameParameterValue") @NotNull String path) {
    return IconLoader.getIcon(path, EduAiHintsIcons.class);
  }
}
