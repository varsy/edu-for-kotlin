package com.jetbrains.edu.learning.authorContentsStorage.zip

import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.*
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.zip.ZipFile

const val COURSE_AUTHOR_CONTENTS_FILE = ".course_author_content.zip"
private val EMPTY_BYTE_ARRAY = byteArrayOf()

/**
 * This is an author contents storage based on a zip file. Zip file should be a physical file on a local file system.
 * It is referenced by a [java.nio.file.Path].
 *
 * This implementation is thread safe: it expects the underlying zip file as immutable, and only reads its contents
 */
open class ZipAuthorContentsStorage @Throws(IOException::class) constructor(protected val zipFilePath: Path) : AuthorContentsStorage {

  open val path2binary: Map<String, Boolean> by lazy {
    evaluateIsBinaryMap()
  }

  protected fun evaluateIsBinaryMap(): Map<String, Boolean> = try {
    ZipFile(zipFilePath.toFile()).use { zipFile ->
      zipFile.entries().asSequence().map { entry ->
        val isBinary = entry.extra?.contentEquals(EXTRA_ENTRY_IS_BINARY) ?: true
        entry.name to isBinary
      }.toMap()
    }
  }
  catch (e: NoSuchFileException) {
    mapOf()
  }

  override fun get(path: String): FileContents? {
    val isBinary = path2binary[path] ?: return null
    return if (isBinary) {
      BinaryContentsFromZipStorage(path)
    }
    else {
      TextualContentsFromZipStorage(path)
    }
  }

  open fun getBytes(path: String): ByteArray = try {
    ZipFile(zipFilePath.toFile()).use { zipFile ->
      val entry = zipFile.getEntry(path)
      return@use zipFile.getInputStream(entry).readAllBytes()
    }
  }
  catch (e: IOException) {
    LOG.error("failed to read author contents from zip: $zipFilePath, path: $path", e)
    EMPTY_BYTE_ARRAY
  }

  inner class TextualContentsFromZipStorage(private val path: String) : TextualContents {
    override val text: String
      get() = String(getBytes(path), Charsets.UTF_8)
  }

  inner class BinaryContentsFromZipStorage(private val path: String) : BinaryContents {
    override val bytes: ByteArray
      get() = getBytes(path)
  }

  companion object {
    val LOG = Logger.getInstance(ZipAuthorContentsStorage::class.java)
  }
}