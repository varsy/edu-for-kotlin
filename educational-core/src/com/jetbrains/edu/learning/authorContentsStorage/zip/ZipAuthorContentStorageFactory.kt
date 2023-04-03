package com.jetbrains.edu.learning.authorContentsStorage.zip

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentStorageFactory
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.isToEncodeContent
import java.io.*
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempFile

/**
 * The contents storage factory to store contents inside a zip file.
 *
 * Instances of this class may be used only in one thread, i.e. in the same thread they were created.
 * The only exception is the access to the [builtStorage] value, it can be accessed from any thread.
 */
class ZipAuthorContentStorageFactory @Throws(IOException::class) constructor(
  private val temporaryZipFilePath: Path = createTempFile("author_contents", ".zip")
) : AuthorContentStorageFactory<ZipAuthorContentsStorage> {

  private val initThread: Thread = Thread.currentThread()

  private val addedPaths = mutableSetOf<String>()

  private val zipOutputStream: ZipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(temporaryZipFilePath.toFile())))

  private var frozen: Boolean = false

  @Volatile
  override var builtStorage: ZipAuthorContentsStorage? = null
    private set

  override fun put(path: String, fileContents: FileContents) {
    ensureInInitThread()
    ensureNotFrozen()

    when (fileContents) {
      is TextualContents -> put(path, fileContents.text.encodeToByteArray())
      is BinaryContents -> put(path, fileContents.bytes)
      is UndeterminedContents -> if (determineBinaryByPath(path)) {
        putWithoutChecks(path, fileContents.interpretAsBinary().bytes)
      }
      else {
        putWithoutChecks(path, fileContents.interpretAsTextual().text.encodeToByteArray())
      }
    }
  }

  override fun put(path: String, bytes: ByteArray) {
    ensureInInitThread()
    ensureNotFrozen()

    putWithoutChecks(path, bytes)
  }

  override fun build(): ZipAuthorContentsStorage {
    ensureInInitThread()
    ensureNotFrozen()

    frozen = true
    try {
      zipOutputStream.close()
    }
    catch (e: IOException) {
      LOG.error("failed to build zip author contents storage", e)
    }
    val builtStorage = ZipAuthorContentsStorage(temporaryZipFilePath)
    this.builtStorage = builtStorage
    return builtStorage
  }

  private fun putWithoutChecks(path: String, byteArray: ByteArray) {
    val entry = ZipEntry(path)
    zipOutputStream.putNextEntry(entry)
    zipOutputStream.write(byteArray)

    addedPaths.add(path)
  }

  private fun ensureNotFrozen() {
    if (frozen) throw IllegalStateException("zip author content storage factory is already frozen")
  }

  private fun ensureInInitThread() {
    val currentThread = Thread.currentThread()
    if (currentThread != initThread) throw IllegalStateException("ZipAuthorContentStorageFactory must be initialized and used from one stream: initialized in $initThread, used from $currentThread")
  }

  private fun determineBinaryByPath(path: String): Boolean = FakeVirtualFile(path).isToEncodeContent

  companion object {
    val LOG = Logger.getInstance(ZipAuthorContentsStorage::class.java)
  }
}

/**
 * The intellij platform is able to determine file types for [VirtualFile]s.
 * It is needed in the plugin to tell binary files from textual files.
 * We do not want to create real virtual files just to get their file type, so we use these [FakeVirtualFile]s.
 */
private class FakeVirtualFile(private val fakePath: String) : VirtualFile() {
  override fun getName(): String {
    val i = fakePath.indexOf('/')
    return if (i > 0) path.substring(i + 1) else path
  }

  override fun getFileSystem(): VirtualFileSystem = LocalFileSystem.getInstance()

  override fun getPath(): String = fakePath

  override fun isWritable(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isDirectory(): Boolean = false

  override fun isValid(): Boolean = true

  override fun getParent(): VirtualFile? = if (this == ROOT) null else ROOT

  override fun getChildren(): Array<VirtualFile> {
    TODO("Not yet implemented")
  }

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
    TODO("Not yet implemented")
  }

  override fun contentsToByteArray(): ByteArray = byteArrayOf()

  override fun getTimeStamp(): Long {
    TODO("Not yet implemented")
  }

  override fun getLength(): Long = 0

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    TODO("Not yet implemented")
  }

  override fun getInputStream(): InputStream {
    TODO("Not yet implemented")
  }

  companion object {
    val ROOT = FakeVirtualFile("/")
  }
}