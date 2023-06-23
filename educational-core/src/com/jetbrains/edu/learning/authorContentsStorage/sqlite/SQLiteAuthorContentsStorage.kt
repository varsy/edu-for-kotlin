package com.jetbrains.edu.learning.authorContentsStorage.sqlite

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.authorContentsStorage.disambiguateContents
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

private const val TABLE_NAME = "FileContents"

class SQLiteAuthorContentsStorage private constructor(private val db: Path) : AuthorContentsStorage {

  val connection: Connection
    get() = DriverManager.getConnection(connectionUrl(db))

  private fun <T> dbAction(action: (Connection) -> T): T = connection.use { connection ->
    action(connection)
  }

  override fun holderForPath(path: String): FileContentsHolder = object : FileContentsHolder {
    override fun get(): FileContents = dbAction { connection ->
      showConnection(connection)

      val getterStatement = connection.prepareStatement("""
        SELECT `textual`, `binary`
        FROM $TABLE_NAME
        WHERE `path`=?
      """)
      getterStatement.setString(1, path)
      val result = getterStatement.executeQuery()
      if (result.next()) {
        val text = result.getString(1)
        if (text != null) {
          return@dbAction TextualContents(text)
        }
        val bytes = result.getBytes(2)
        if (bytes != null) {
          return@dbAction BinaryContents(bytes)
        }
      }

      return@dbAction UndeterminedContents.EMPTY
    }

    override fun set(value: FileContents) = dbAction { connection ->
      val update = connection.prepareStatement("""
        INSERT OR REPLACE INTO `$TABLE_NAME`(`path`, `textual`, `binary`)
        VALUES (?, ?, ?)
        """
      )

      showConnection(connection)

      update.setString(1, path)
      when (val determinedContents = disambiguateContents(value, path)) {
        is TextualContents -> {
          update.setString(2, determinedContents.text) //TODO does it work for strings > 32kb ?
          update.setNull(3, Types.BLOB)
        }
        is BinaryContents -> {
          update.setNull(2, Types.VARCHAR)
          update.setBytes(3, determinedContents.bytes)
        }
      }

      update.executeUpdate()

      showConnection(connection)
    }
  }

  override fun update(course: Course) {
    // do nothing, no need to update, all data is already stored where needed
  }

  private fun ensureDBCreated() = dbAction { connection ->
    val createTableStatement = connection.createStatement()
    createTableStatement.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS `$TABLE_NAME` (
        `path` TEXT PRIMARY KEY,
        `textual` CLOB,
        `binary` BLOB
      )
      """
    )
  }

  companion object {
    fun openOrCreateDB(db: Path): SQLiteAuthorContentsStorage {
      val storage = SQLiteAuthorContentsStorage(db)

      storage.ensureDBCreated()

      return storage
    }

    fun openTemporaryDB(): SQLiteAuthorContentsStorage = openOrCreateDB(kotlin.io.path.createTempFile("temp-storage", ".db"))

    private fun connectionUrl(path: Path) = "jdbc:sqlite:${path}"
  }
}

private fun showConnection(connection: Connection) {
  val q = connection.createStatement()
  val result = q.executeQuery("SELECT * FROM $TABLE_NAME")
  while (result.next()) {
    val a = result.getString(1)
    val b = result.getString(2)
    val c = result.getBytes(3)
    logger<SQLiteAuthorContentsStorage>().info("Table: $a $b $c")
  }
}