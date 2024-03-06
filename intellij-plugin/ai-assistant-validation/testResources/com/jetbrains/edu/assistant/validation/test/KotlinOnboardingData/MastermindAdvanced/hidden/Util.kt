package jetbrains.kotlin.course.mastermind.advanced

fun safeReadLine() = readlnOrNull() ?: error("Your input is incorrect, sorry")

val newLineSymbol: String = System.lineSeparator()