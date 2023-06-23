package com.jetbrains.edu.learning.courseFormat

interface FileContentsHolder {
  fun get(): FileContents
  fun set(value: FileContents)
}

class InMemoryFileContentsHolder(@Volatile private var fileContents: FileContents = UndeterminedContents.EMPTY) : FileContentsHolder {

  override fun get(): FileContents = fileContents

  override fun set(value: FileContents) {
    fileContents = value
  }
}