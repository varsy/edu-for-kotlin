package com.jetbrains.edu.cognifire.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

public final class CognifireIcons {
  public static final Icon Sync = load("/icons/cognifire/sync.svg");

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@SuppressWarnings("SameParameterValue") @NotNull String path) {
    return IconLoader.getIcon(path, CognifireIcons.class);
  }
}
