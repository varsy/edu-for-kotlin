package com.jetbrains.edu.learning.authorContentsStorage

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.authorContentsStorage.zip.ZipAuthorContentsStorageFactory
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.InMemoryUndeterminedContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.LazyFileContents
import java.nio.file.Files
import java.util.*
import kotlin.io.path.createTempFile

class AuthorContentsStorageTests : EduTestCase() {

  fun `test author contents storage can save and restore file contents`() {
    val zipFile = createTempFile("author_contents", ".zip")

    try {
      val storageFactory = ZipAuthorContentsStorageFactory(zipFile)

      fillStorageFactoryWithExampleContents(storageFactory)

      val storage = storageFactory.build()

      assertThrows(IllegalStateException::class.java) {
        //we are not allowed to build two times
        storageFactory.build()
      }

      assertEquals(storage, storageFactory.builtStorage)

      assertFileContentsEqual(InMemoryTextualContents("hello from a.txt"), storage.get("a.txt"))
      assertFileContentsEqual(InMemoryTextualContents("hello from b.txt"), storage.get("folder/b.txt"))
      assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(10, 20, 30)), storage.get("image.png"))
      // in ZipAuthorContentsStorage, undefined contents are disambiguated at the moment of writing, so we have no more undefined contents
      assertFileContentsEqual(InMemoryTextualContents("hello from undefined.txt"), storage.get("undefined.txt"))
      assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(10, 20, 30)), storage.get("undefined.png"))
      assertFileContentsEqual(null, storage.get("bwv999.mp3"))
    }
    finally {
      Files.delete(zipFile)
    }
  }

  fun `test lazy file contents correctly delegates to the contents storage`() {
    val zipFile = createTempFile("author_contents", ".zip")

    try {
      val storageFactory = ZipAuthorContentsStorageFactory(zipFile)

      // create a number of lazy file contents. They will point to the storage that is not already built.
      val lazyATxt = LazyFileContents {
        storageFactory.builtStorage?.get("a.txt")
      }
      val lazyBTxt = LazyFileContents {
        storageFactory.builtStorage?.get("folder/b.txt")
      }
      val lazyImagePng = LazyFileContents {
        storageFactory.builtStorage?.get("image.png")
      }
      val lazyUndefinedTxt = LazyFileContents {
        storageFactory.builtStorage?.get("undefined.txt")
      }
      val lazyUndefinedPng = LazyFileContents {
        storageFactory.builtStorage?.get("undefined.png")
      }
      val lazyNothing = LazyFileContents {
        storageFactory.builtStorage?.get("bwv999.mp3")
      }

      fillStorageFactoryWithExampleContents(storageFactory)
      storageFactory.build()

      assertFileContentsEqual(InMemoryTextualContents("hello from a.txt"), lazyATxt)
      assertFileContentsEqual(InMemoryTextualContents("hello from b.txt"), lazyBTxt)
      assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(10, 20, 30)), lazyImagePng)
      // in ZipAuthorContentsStorage, undefined contents are disambiguated at the moment of writing, so we have no more undefined contents
      assertFileContentsEqual(InMemoryTextualContents("hello from undefined.txt"), lazyUndefinedTxt)
      assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(10, 20, 30)), lazyUndefinedPng)
      assertFileContentsEqual(UndeterminedContents.EMPTY, lazyNothing)
    }
    finally {
      Files.delete(zipFile)
    }
  }

  private fun fillStorageFactoryWithExampleContents(storageFactory: ZipAuthorContentsStorageFactory) {
    storageFactory.put("a.txt", InMemoryTextualContents("hello from a.txt"))
    storageFactory.put("folder/b.txt", InMemoryTextualContents("hello from b.txt"))
    storageFactory.put("image.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
    storageFactory.put("undefined.txt", InMemoryUndeterminedContents("hello from undefined.txt"))
    storageFactory.put("undefined.png", InMemoryUndeterminedContents(Base64.getEncoder().encodeToString(byteArrayOf(10, 20, 30))))
  }
}