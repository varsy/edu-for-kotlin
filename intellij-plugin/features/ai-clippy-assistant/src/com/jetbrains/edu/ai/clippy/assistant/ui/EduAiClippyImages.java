package com.jetbrains.edu.ai.clippy.assistant.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@SuppressWarnings("SameParameterValue")
public final class EduAiClippyImages {
  public static final Icon Clippy = load("/images/clippy.svg");
  public static final Icon Frog = load("/images/frog.png");

  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EduAiClippyImages.class);
  }
}
