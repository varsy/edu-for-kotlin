package com.jetbrains.edu.learning.authorContentsStorage.zip

import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.*
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.exists

const val COURSE_AUTHOR_CONTENTS_FILE = ".course_author_content.zip"
private val EMPTY_BYTE_ARRAY = byteArrayOf()

/**
 * This is an author contents storage based on a zip file. Zip file should be a physical file on a local file system.
 * It is referenced by a [java.nio.file.Path].
 */
class ZipAuthorContentsStorage @Throws(IOException::class) constructor(private val zipFilePath: Path) : AuthorContentsStorage {

  private val path2binary: Map<String, Boolean> by lazy {
    ZipFile(zipFilePath.toFile()).use { zipFile ->
      zipFile.entries().asSequence().map { entry ->
        val isBinary = entry.extra?.contentEquals(EXTRA_ENTRY_IS_BINARY) ?: true
        entry.name to isBinary
      }.toMap()
    }
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

  fun getBytes(path: String): ByteArray = try {
    ZipFile(zipFilePath.toFile()).use { zipFile ->
      val entry = zipFile.getEntry(path)
      return@use zipFile.getInputStream(entry).readAllBytes()
    }
  } catch (e: IOException) {
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

    private val courseDir2zipDir = mutableMapOf<VirtualFile, Path>()

    /**
     * This method gets author contents storage, that corresponds to a [course] in a project located in the [courseDir].
     * If this folder contains the file `.course_author_content.zip`, then the AuthorContentsStorage is
     * a wrapper for that file.
     * If this folder does not contain the file, then this file is created, and the contents of all the `TaskFiles` and additional files
     * from the [course] are packed inside that file.
     */
    @JvmStatic
    fun initAuthorContentsStorage(course: Course?, courseDir: VirtualFile?): AuthorContentsStorage? {
      if (course == null || courseDir == null) return null
      if (!course.isStudy) return null

      val projectDirectory: VirtualFile = courseDir
      val zipPath = getZipPathForProjectFolder(projectDirectory)
      if (zipPath.exists()) return ZipAuthorContentsStorage(zipPath)

      val factory = ZipAuthorContentStorageFactory(zipPath)
      course.visitTasks { task ->
        for (taskFile in task.taskFiles.values) {
          factory.put(pathInAuthorContentsStorageForEduFile(taskFile), taskFile.contents)
        }
      }
      for (additionalFile in course.additionalFiles) {
        factory.put(pathInAuthorContentsStorageForEduFile(additionalFile), additionalFile.contents)
      }
      return factory.build()
    }

    /**
     * Returns the [Path] to the zip file with the author contents storage of a project, located in the [projectDirectory] directory.
     * Usually, that is a `.course_author_content.zip` in the project directory.
     * But sometimes in tests, the file system is not real, and it is not possible to get a Path inside that file system.
     * So, the temporary file on a real file system is created.
     *
     * We store that temporary file for a project in a [courseDir2zipDir] map.
     */
    private fun getZipPathForProjectFolder(projectDirectory: VirtualFile): Path {
      val fileSystem: VirtualFileSystem = projectDirectory.fileSystem

      val projectDirectoryPath = fileSystem.getNioPath(projectDirectory)

      // projectDirectoryPath is null if fileSystem is temporary, i.e. we are in the unit test mode.
      val zipFileContainingDirectory = projectDirectoryPath ?: courseDir2zipDir.computeIfAbsent(projectDirectory) {
        Files.createTempDirectory("boundAuthorContentStorage")
      }
      return zipFileContainingDirectory.resolve(COURSE_AUTHOR_CONTENTS_FILE)
    }
  }
}