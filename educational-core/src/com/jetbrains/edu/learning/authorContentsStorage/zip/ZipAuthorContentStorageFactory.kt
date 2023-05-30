package com.jetbrains.edu.learning.authorContentsStorage.zip

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentStorageFactory
import com.jetbrains.edu.learning.isToEncodeContent
import java.io.*
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempFile

val EXTRA_ENTRY_IS_BINARY = byteArrayOf(1)
val EXTRA_ENTRY_IS_TEXTUAL = byteArrayOf(2)

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

  init {
    LOG.info("Created ZipAuthorContentStorageFactory to write in $temporaryZipFilePath, thread: $initThread")
  }

  private val startTime: Long = System.nanoTime()

  private val addedPaths = mutableSetOf<String>()

  private val zipOutputStream: ZipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(temporaryZipFilePath.toFile())))

  private var frozen: Boolean = false

  @Volatile
  override var builtStorage: ZipAuthorContentsStorage? = null
    private set

  /**
   * Puts the contents inside an archive.
   * This storage does not store the contents with the undefined type, i.e. with [FileContents.isBinary] field equal to `null`.
   * It is possible to store such contents, but we want to resolve the ambiguity as early as possible.
   * The contents of the undefined type are first disambiguated based on the file name, and then stored either as binary or textual.
   */
  override fun put(path: String, fileContents: FileContents) {
    ensureInInitThread()
    ensureNotFrozen()

    when (fileContents.isBinary) {
      false -> putWithoutChecks(path, (fileContents as TextualContents).text.encodeToByteArray(), false)
      true -> putWithoutChecks(path, (fileContents as BinaryContents).bytes, true)
      null -> if (determineBinaryByPath(path)) {
        putWithoutChecks(path, (fileContents as BinaryContents).bytes, true)
      }
      else {
        putWithoutChecks(path, (fileContents as TextualContents).text.encodeToByteArray(), false)
      }
    }
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

    val nanoTime = System.nanoTime() - startTime
    LOG.info("Finished building ZipAuthorContentsStorage in $temporaryZipFilePath, thread $initThread. Elapsed nano time: $nanoTime")

    return builtStorage
  }

  private fun putWithoutChecks(path: String, byteArray: ByteArray, isBinary: Boolean) {
    val entry = ZipEntry(path)
    entry.extra = if (isBinary) EXTRA_ENTRY_IS_BINARY else EXTRA_ENTRY_IS_TEXTUAL
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
 *
 * A number of methods are not implemented because they are not called (hopefully) during determining the file type.
 */
private class FakeVirtualFile(private val fakePath: String) : VirtualFile() {
  override fun getName(): String {
    val i = fakePath.lastIndexOf('/')
    return if (i > 0) path.substring(i + 1) else path
  }

  override fun getFileSystem(): VirtualFileSystem = LocalFileSystem.getInstance()

  override fun getPath(): String = fakePath

  override fun isWritable(): Boolean = TODO("This method is not expected to be called")

  override fun isDirectory(): Boolean = false

  override fun isValid(): Boolean = true

  /**
   * Sometimes, to determine the type, the platform examines the name of the parent folder.
   * That's why we want to act as if we really get the parent folder
   */
  override fun getParent(): VirtualFile? {
    if (this == ROOT) return null

    val i = fakePath.lastIndexOf('/')
    return if (i <= 0) ROOT else FakeVirtualFile(fakePath.substring(0, i))
  }

  override fun getChildren(): Array<VirtualFile> = TODO("This method is not expected to be called")

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream = TODO("This method is not expected to be called")

  override fun contentsToByteArray(): ByteArray = byteArrayOf()

  override fun getTimeStamp(): Long = TODO("This method is not expected to be called")

  override fun getLength(): Long = 0

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) = TODO("This method is not expected to be called")

  override fun getInputStream(): InputStream = TODO("This method is not expected to be called")

  companion object {
    val ROOT = FakeVirtualFile("/")
  }
}