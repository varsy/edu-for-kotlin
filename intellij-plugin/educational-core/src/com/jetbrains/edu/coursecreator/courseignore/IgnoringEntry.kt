package com.jetbrains.edu.coursecreator.courseignore

import org.intellij.lang.annotations.Language

/**
 * Several patterns for a `.courseignore` file, that together form a piece of ignoring logic (such as ignore all java binaries).
 * [comment] an explanation comment
 * [courseignoreLines] several patterns, without indentation
 *
 * In future, [IgnoringEntry] will also contain logic to test the `.courseignore` file, if it actually ignores all the needed files.
 * This is needed for inspections.
 * It will also contain information to support `.courseignore` migrations
 */
class IgnoringEntry(val comment: String?, val courseignoreLines: List<String>)

fun ignoringEntry(comment: String, @Language("CourseIgnore") ignoreEntries: String) =
  IgnoringEntry(comment, ignoreEntries.split("\n").map { it.trim() }.filter { it.isNotEmpty() })