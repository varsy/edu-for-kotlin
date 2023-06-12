package com.jetbrains.edu.learning.authorContentsStorage.sqlite

import com.jetbrains.edu.learning.authorContentsStorage.disambiguateContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.fileContents.BinaryContents
import com.jetbrains.edu.learning.courseFormat.fileContents.FileContents
import com.jetbrains.edu.learning.courseFormat.fileContents.TextualContents
import com.jetbrains.edu.learning.courseFormat.fileContents.UndeterminedContents
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

private const val TABLE_NAME = "FileContents"

/**
 * [AuthorContentsStorage] that stores [FileContents] in a SQLite database.
 * The SQLite database is embedded and has only one file on disk with all the database data.
 * The [db] argument is a path on the local file system to the file with the database.
 *
 * The database contains one table with the columns:
 *  * path: Boolean (primary key)
 *  * textual: String
 *  * binary: BLOB
 *
 * Either `textual` or `binary` column is used, the other is NULL.
 * The storage does not store [UndeterminedContents], so it disambiguates it before storing.
 */
class SQLiteAuthorContentsStorage private constructor(val db: Path) : AuthorContentsStorage {

  val connection: Connection
    get() {
      return DriverManager.getConnection(connectionUrl(db))
    }

  private fun <T> dbAction(action: (Connection) -> T): T = connection.use { connection ->
    action(connection)
  }

  override fun put(path: String, contents: FileContents): Unit = dbAction { connection ->
    val putStatement = connection.prepareStatement("""
        INSERT OR REPLACE INTO `$TABLE_NAME`(`path`, `textual`, `binary`)
        VALUES (?, ?, ?)
        """
    )

    putStatement.setString(1, path)
    when (val determinedContents = disambiguateContents(contents, path)) {
      is TextualContents -> {
        putStatement.setString(2, determinedContents.text) //TODO does it work for strings > 32kb ?
        putStatement.setNull(3, Types.BLOB)
      }
      is BinaryContents -> {
        putStatement.setNull(2, Types.VARCHAR)
        putStatement.setBytes(3, determinedContents.bytes)
      }
    }

    putStatement.executeUpdate()
  }


  override fun get(path: String): FileContents = dbAction { connection ->
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
    init {
      //Make sure the JDBC driver for SQLite is loaded
      Class.forName("org.sqlite.JDBC")
    }

    /**
     * Opens a storage for a db-file with the already known path.
     */
    fun openOrCreateDB(db: Path): SQLiteAuthorContentsStorage {
      val storage = SQLiteAuthorContentsStorage(db)

      storage.ensureDBCreated()

      return storage
    }

    /**
     * Opens a storage with some temporary file
     */
    fun openTemporaryDB(): SQLiteAuthorContentsStorage {
      val tempDB = kotlin.io.path.createTempFile("temp-storage", ".db")
      tempDB.toFile().deleteOnExit()
      return openOrCreateDB(tempDB)
    }

    private fun connectionUrl(path: Path) = "jdbc:sqlite:${path}"
  }
}