package com.jetbrains.edu.learning.courseFormat.fileContents

class InMemoryFileContentsHolder(@Volatile private var fileContents: FileContents = UndeterminedContents.EMPTY) : FileContentsHolder {

  override fun get(): FileContents = fileContents

  override fun set(value: FileContents) {
    fileContents = value
  }
}