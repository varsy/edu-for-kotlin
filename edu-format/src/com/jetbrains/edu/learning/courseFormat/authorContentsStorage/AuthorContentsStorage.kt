package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FileContentsHolder

/**
 * After a student creates a project for some course, he or she may modify the created files.
 * Sometimes, we need to have initial versions of files created by a course author.
 * Use cases include:
 *    1. Reverting a student's solution;
 *    2. Moving to the next step in a framework lesson;
 *    3. Hiding test files and restoring them again before running tests;
 *
 * Objects implementing [AuthorContentsStorage] correspond to some persistent storage of
 * files with the initial version of the course files.
 */
interface AuthorContentsStorage {
  fun holderForPath(path: String): FileContentsHolder

  fun put(path: String, contents: FileContents) = holderForPath(path).set(contents)
  fun get(path: String): FileContents = holderForPath(path).get()

  /**
   * ALl the contents of edu files from the [course] are saved inside this storage, no other contents are preserved.
   */
  fun update(course: Course)
}