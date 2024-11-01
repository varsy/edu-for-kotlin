package com.jetbrains.edu.ai.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@SuppressWarnings("unused")
public final class EducationalAIIcons {
  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EducationalAIIcons.class);
  }

  public static final Icon Translation = load("/icons/com/jetbrains/edu/ai/actions/translation.svg");
  public static final Icon TranslationEnabled = load("/icons/com/jetbrains/edu/ai/actions/translation_enabled.svg");
  public static final Icon TranslationHovered = load("/icons/com/jetbrains/edu/ai/actions/translation_hovered.svg");
  public static final Icon TranslationPressed = load("/icons/com/jetbrains/edu/ai/actions/translation_pressed.svg");
}
