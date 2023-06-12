package com.jetbrains.edu.learning.authorContentsStorage

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.authorContentsStorage.sqlite.SQLiteAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.fileContents.BinaryContents
import com.jetbrains.edu.learning.courseFormat.fileContents.TextualContents
import com.jetbrains.edu.learning.courseFormat.fileContents.UndeterminedContents
import java.nio.file.Files
import java.util.*
import kotlin.io.path.createTempFile

class AuthorContentsStorageTests : EduTestCase() {

  fun `test author contents storage can save and restore file contents`() {
    val dbFile = createTempFile("author_contents", ".zip")

    try {
      val storage = SQLiteAuthorContentsStorage.openOrCreateDB(dbFile)

      storage.put("a.txt", TextualContents("hello from a.txt"))
      storage.put("folder/b.txt", TextualContents("hello from b.txt"))
      storage.put("image.png", BinaryContents(byteArrayOf(10, 20, 30)))
      storage.put("undefined.txt", UndeterminedContents("hello from undefined.txt"))
      storage.put("undefined.png", UndeterminedContents(Base64.getEncoder().encodeToString(byteArrayOf(10, 20, 30))))

      assertEquals(TextualContents("hello from a.txt"), storage.get("a.txt"))
      assertEquals(TextualContents("hello from b.txt"), storage.get("folder/b.txt"))
      assertEquals(BinaryContents(byteArrayOf(10, 20, 30)), storage.get("image.png"))
      // in SQLiteAuthorContentsStorage, undefined contents are disambiguated at the moment of writing; so we have no more undefined contents
      assertEquals(TextualContents("hello from undefined.txt"), storage.get("undefined.txt"))
      assertEquals(BinaryContents(byteArrayOf(10, 20, 30)), storage.get("undefined.png"))
      assertEquals(UndeterminedContents.EMPTY, storage.get("bwv999.mp3"))
    }
    finally {
      Files.delete(dbFile)
    }
  }

}