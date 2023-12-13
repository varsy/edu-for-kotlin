package com.jetbrains.edu.learning.authorContentsStorage

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import org.jetbrains.sqlite.ObjectBinder
import org.jetbrains.sqlite.SqliteConnection
import java.nio.file.Path

private const val TABLE_NAME = "Contents"

class SQLiteContentsStorage(val db: Path) : Disposable {

  private val connection = SqliteConnection(db)
  private val getStatement = connection.statementPool(sql= "SELECT `value` FROM $TABLE_NAME WHERE `key`=?") { ObjectBinder(1) }
  private val putStatement = connection.statementPool(sql="INSERT OR REPLACE INTO `$TABLE_NAME`(`key`, `value`) VALUES (?, ?)") { ObjectBinder(2) }

  fun get(key: String): ByteArray = getStatement.use { statement, binder ->
    binder.bind(key)
    val resultSet = statement.executeQuery()
    resultSet.next()
    resultSet.getBytes(0) ?: byteArrayOf()
  }

  fun put(key: String, value: ByteArray): Unit = putStatement.use { statement, binder ->
    binder.bind(key, value)
    statement.executeUpdate()
  }

  fun put(key: String, value: String): Unit = put(key, value.byteInputStream().readAllBytes())

  private fun createDB() = connection.execute("""
    CREATE TABLE IF NOT EXISTS `$TABLE_NAME` (
      `key` TEXT PRIMARY KEY,
      `value` BLOB
    )
  """)

  override fun dispose() = connection.interruptAndClose()

  companion object {
    fun openOrCreateDB(db: Path): SQLiteContentsStorage {
      val storage = SQLiteContentsStorage(db)
      storage.createDB()
      return storage
    }
  }
}

interface SQLiteContents {
  val storage: SQLiteContentsStorage
  val path: String
}

class SQLiteTextualContents(override val storage: SQLiteContentsStorage, override val path: String): TextualContents, SQLiteContents {
  override val text: String
    get() = String(storage.get(path))
}

class SQLiteBinaryContents(override val storage: SQLiteContentsStorage, override val path: String): BinaryContents, SQLiteContents {
  override val bytes: ByteArray
    get() = storage.get(path)
}

class SQLiteUndeterminedContents(override val storage: SQLiteContentsStorage, override val path: String): UndeterminedContents, SQLiteContents {
  override val textualRepresentation: String
    get() = String(storage.get(path))
}