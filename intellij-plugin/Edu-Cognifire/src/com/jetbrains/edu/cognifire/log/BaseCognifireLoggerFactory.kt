package com.jetbrains.edu.cognifire.log

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.JulLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.RollingFileHandler
import com.intellij.openapi.util.io.findOrCreateFile
import java.nio.file.Path
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.io.path.div
import kotlin.io.path.exists

class BaseCognifireLoggerFactory(private val category: String) : Logger.Factory {
  private val basePath: Path = Path.of(PathManager.getPluginsPath()) / "cognifire.log"

  private val appender: RollingFileHandler = RollingFileHandler(basePath, 20_000_000, 50, false)

  init {
    try {
      runWriteAction {
        basePath.findOrCreateFile()
      }
    } catch (e: Throwable) {
      LOG.error("Could not create a log file for $category logs; path: $basePath", e)
    }
    LOG.info("$category logs are written to $basePath")
  }

  override fun getLoggerInstance(category: String): Logger {
    require(category == this.category)
    val logger = java.util.logging.Logger.getLogger(category)
    logger.addHandler(appender)
    appender.formatter = object : Formatter() {
      override fun format(record: LogRecord) = String.format("%1\$10tT %2\$-5s - %3\$s%n", record.millis, record.level, record.message)
    }
    logger.useParentHandlers = false
    logger.level = Level.INFO
    return JulLogger(logger)
  }

  fun getLoggerInstanceOrNull(): Logger? = if (basePath.exists()) {
    getLoggerInstance(category)
  } else {
    null
  }

  companion object {
    private val LOG = Logger.getInstance(BaseCognifireLoggerFactory::class.java)
  }
}
