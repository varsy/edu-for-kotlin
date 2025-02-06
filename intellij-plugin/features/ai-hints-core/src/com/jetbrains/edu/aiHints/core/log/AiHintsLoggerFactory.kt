package com.jetbrains.edu.aiHints.core.log

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

data object AiHintsLoggerFactory : Logger.Factory {
  private const val CATEGORY: String = "AI-Hints"
  private val basePath: Path = Path.of(PathManager.getPluginsPath()) / "ai-hints" / "aiHints.log"

  private val LOG = Logger.getInstance(AiHintsLoggerFactory::class.java)

  private val appender: RollingFileHandler = RollingFileHandler(basePath, 20_000_000, 50, false)

  private val formatter: Formatter = object : Formatter() {
    override fun format(record: LogRecord) = String.format(
      "%1\$tF %1\$tT.%1\$tL %1\$tz | %2\$-5s %3\$s%n",
      record.millis,
      record.level,
      record.message.replace("\n", "\\n")
    )
  }

  init {
    try {
      runWriteAction {
        basePath.findOrCreateFile()
      }
    } catch (e: Throwable) {
      LOG.error("Could not create a log file for $CATEGORY logs; path: $basePath", e)
    }
    LOG.info("$CATEGORY logs are written to $basePath")
  }

  override fun getLoggerInstance(category: String): Logger {
    require(category == this.CATEGORY)
    val logger = java.util.logging.Logger.getLogger(category)
    logger.addHandler(appender)
    appender.formatter = formatter
    logger.useParentHandlers = false
    logger.level = Level.INFO
    return JulLogger(logger)
  }

  fun getLoggerInstanceOrNull(): Logger? =
    if (basePath.exists()) {
      getLoggerInstance(CATEGORY)
    } else {
      null
    }
}
