package jetbrains.kotlin.course.warmup

fun getGameRules(wordLength: Int, maxAttemptsCount: Int, secretExample: String) =
  "Welcome to the game! $newLineSymbol" +
  newLineSymbol +
  "Two people play this game: one chooses a word (a sequence of letters), " +
  "the other guesses it. In this version, the computer chooses the word: " +
  "a sequence of $wordLength letters (for example, $secretExample). " +
  "The user has several attempts to guess it (the max number is $maxAttemptsCount). " +
  "For each attempt, the number of complete matches (letter and position) " +
  "and partial matches (letter only) is reported. $newLineSymbol" +
  newLineSymbol +
  "For example, with $secretExample as the hidden word, the BCDF guess will " +
  "give 1 full match (C) and 1 partial match (B)."

fun countPartialMatches(secret: String, guess: String): Int = TODO("Not implemented yet")

fun countExactMatches(secret: String, guess: String): Int = TODO("Not implemented yet")

fun generateSecret() = "ABCD"

fun isComplete(secret: String, guess: String) = true

fun main() {
  val wordLength = 4
  val maxAttemptsCount = 3
  val secretExample = "ACEB"
  println(getGameRules(wordLength, maxAttemptsCount, secretExample))
}