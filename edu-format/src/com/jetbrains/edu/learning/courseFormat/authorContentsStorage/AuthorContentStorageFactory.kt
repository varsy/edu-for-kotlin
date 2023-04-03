package com.jetbrains.edu.learning.courseFormat.authorContentsStorage

import com.jetbrains.edu.learning.courseFormat.FileContents

/**
 * A factory to create instances of [AuthorContentsStorage].
 */
interface AuthorContentStorageFactory<T: AuthorContentsStorage> {
  fun put(path: String, fileContents: FileContents)
  fun put(path: String, bytes: ByteArray)

  /**
   * Creates an instance of the [AuthorContentsStorage].
   * Behaviour is undefined if the method is called several times.
   */
  fun build(): T

  /**
   * The instance of the [AuthorContentsStorage], that was previously built by the [build()] method.
   * It is null if the instance has not been already built.
   */
  val builtStorage: AuthorContentsStorage?
}