package com.jetbrains.edu.learning.courseFormat.fileContents

/**
 * A holder for [FileContents] that makes it possible for file contents to be stored in different places: in memory, in DB,
 * on disk.
 */
interface FileContentsHolder {
  fun get(): FileContents
  fun set(value: FileContents)
}

