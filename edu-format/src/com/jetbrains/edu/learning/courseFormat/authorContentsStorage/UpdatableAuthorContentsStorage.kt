package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.Course

/**
 * This author contents storage may be updated. The only update possibility is represented with the [update] method.
 */
interface UpdatableAuthorContentsStorage : AuthorContentsStorage {

  /**
   * ALl the contents of edu files from the [course] are saved inside this storage, no other contents are preserved.
   */
  fun update(course: Course)

}