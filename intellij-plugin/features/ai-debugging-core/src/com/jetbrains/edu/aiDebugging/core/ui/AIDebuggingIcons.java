package com.jetbrains.edu.aiDebugging.core.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class AIDebuggingIcons {
  public static final Icon AIHint = load("/icons/hint.svg");

  public static final Icon AIBug = load("/icons/bug.svg");

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@SuppressWarnings("SameParameterValue") @NotNull String path) {
    return IconLoader.getIcon(path, AIDebuggingIcons.class);
  }
}
