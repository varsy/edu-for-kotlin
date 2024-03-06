package jetbrains.kotlin.course.warmup

fun safeReadLine() = readlnOrNull() ?: error("Your input is incorrect, sorry")

val newLineSymbol: String = System.lineSeparator()