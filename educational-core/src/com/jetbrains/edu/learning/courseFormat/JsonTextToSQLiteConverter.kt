package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.NioFiles
import com.jetbrains.edu.learning.authorContentsStorage.SQLiteBinaryContents
import com.jetbrains.edu.learning.authorContentsStorage.SQLiteContentsStorage
import com.jetbrains.edu.learning.authorContentsStorage.SQLiteTextualContents
import com.jetbrains.edu.learning.authorContentsStorage.SQLiteUndeterminedContents
import java.io.IOException
import java.util.*

/**
 * A temporary storage for texts read from JSON.
 * Texts are stored in a temporary created SQLite DB.
 * The storage should be disposed for the temporary file to be deleted.
 */
class JsonTextToSQLiteConverter : JsonTextConverter, AutoCloseable {

  private val storage = SQLiteContentsStorage.openOrCreateDB(
    kotlin.io.path.createTempFile("course-contents")
  )

  override fun getContents(textualRepresentation: String, isBinary: Boolean?): FileContents {
    val randomPath = UUID.randomUUID().toString()

    return when (isBinary) {
      true -> {
        storage.put(randomPath, Base64.getDecoder().decode(textualRepresentation))
        SQLiteBinaryContents(storage, randomPath)
      }

      false -> {
        storage.put(randomPath, textualRepresentation)
        SQLiteTextualContents(storage, randomPath)
      }

      null -> {
        storage.put(randomPath, textualRepresentation)
        SQLiteUndeterminedContents(storage, randomPath)
      }
    }
  }

  override fun close() {
    Disposer.dispose(storage)
    try {
      // not a regular Files.deleteIfExists; to use a repeated delete operation to overcome possible issues on Windows
      NioFiles.deleteRecursively(storage.db)
    }
    catch (_: IOException) {}
  }
}