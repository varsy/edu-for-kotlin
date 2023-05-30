package com.jetbrains.edu.learning.authorContentsStorage.zip

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.edu.learning.authorContentsStorage.zip.UpdatableZipAuthorContentsStorage.Companion.courseDir2zipDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.UpdatableAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * This is a [ZipAuthorContentsStorage] extended with the ability to update the archive.
 * Thread safety is taken into account with the [ReentrantReadWriteLock].
 *
 * @param projectDirectory is the directory, in which the zip file for the storage is created.
 * Temporary zip files for the update are also created in this directory, this makes it more
 * probable that the move FS operation will be able to rename temporary file over the permanent file.
 */
class UpdatableZipAuthorContentsStorage(projectDirectory: VirtualFile) : ZipAuthorContentsStorage(
  getZipPathForProjectDirectory(projectDirectory)
), UpdatableAuthorContentsStorage {

  private val readWriteLock = ReentrantReadWriteLock()
  private val readLock = readWriteLock.readLock()
  private val writeLock = readWriteLock.writeLock()

  /**
   * This is a map to store which files inside the archive are binary, which are not.
   * This map is evaluated in two situations: during the object instantiation and during the [update].
   * The [update] is done under the write lock, so this var is evaluated thead safely.
   */
  private var myPath2binary: Map<String, Boolean> = evaluateIsBinaryMap()

  override val path2binary: Map<String, Boolean>
    get() = myPath2binary

  /**
   * This is the archive reading method, it is overriden to work under the read lock.
   */
  override fun getBytes(path: String): ByteArray = readLock.withLock {
    return super.getBytes(path)
  }

  /**
   * This update is working as follows: the contents of all edu files is stored inside a new temporary zip file.
   * Then, under the write lock, the temporary file is moved onto the
   */
  override fun update(course: Course) {
    LOG.info("updating author contents storage")
    val zipDirectory = zipFilePath.parent
    val tempZipPath = Files.createTempFile(zipDirectory, COURSE_AUTHOR_CONTENTS_FILE.substringBefore(".zip"), ".zip")

    saveCourseToZip(tempZipPath, course)

    writeLock.withLock {
      Files.move(tempZipPath, zipFilePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      myPath2binary = evaluateIsBinaryMap()
    }
  }

  private fun saveCourseToZip(zipPath: Path, course: Course) {
    val factory = ZipAuthorContentStorageFactory(zipPath)

    course.visitEduFiles { eduFile ->
      factory.put(pathInAuthorContentsStorageForEduFile(eduFile), eduFile.contents)
    }

    factory.build()
  }

  companion object {
    private val DEFAULT_EMPTY_MAP: Map<String, Boolean> = mapOf()

    private val courseDir2zipDir = mutableMapOf<VirtualFile, Path>()

    /**
     * Returns the [Path] to the zip file with the author contents storage of a project, located in the [projectDirectory] directory.
     * Usually, that is a `.course_author_content.zip` in the project directory.
     * But sometimes in tests, the file system is not real, and it is not possible to get a Path inside that file system.
     * So, the temporary file on a real file system is created.
     *
     * We store that temporary file for a project in a [courseDir2zipDir] map.
     */
    @JvmStatic
    fun getZipPathForProjectDirectory(projectDirectory: VirtualFile): Path {
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