package jetbrains.kotlin.course.mastermind.advanced

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

fun countPartialMatches(secret: String, guess: String): Int {
  val matches = minOf(
    secret.filter { it in guess }.length,
    guess.filter { it in secret }.length,
  )
  return matches - countExactMatches(guess, secret)
}

fun countExactMatches(secret: String, guess: String): Int =
  guess.filterIndexed { index, letter -> letter == secret[index] }.length

fun generateSecret(wordLength: Int, alphabet: String) =
  List(wordLength) { alphabet.random() }.joinToString("")

fun isComplete(secret: String, guess: String) = secret == guess

fun printRoundResults(secret: String, guess: String) {
  val fullMatches = countExactMatches(secret, guess)
  val partialMatches = countPartialMatches(secret, guess)
  println("Your guess has $fullMatches full matches and $partialMatches partial matches.")
}

fun isWon(complete: Boolean, attempts: Int, maxAttemptsCount: Int) = complete && attempts <= maxAttemptsCount

fun isLost(complete: Boolean, attempts: Int, maxAttemptsCount: Int) = !complete && attempts > maxAttemptsCount

fun isCorrectInput(userInput: String, wordLength: Int, alphabet: String): Boolean {
  if (userInput.length != wordLength) {
    println("The length of your guess should be $wordLength! Try again!")
    return false
  }
  val notAlphabetSymbols = userInput.filter { it !in alphabet }
  if (notAlphabetSymbols.isNotEmpty()) {
    println("All symbols in your guess should be from the alphabet: $alphabet! Try again!")
    return false
  }
  return true
}

fun safeUserInput(wordLength: Int, alphabet: String): String {
  var guess: String
  var isCorrect: Boolean
  do {
    println("Please input your guess. It should be of length $wordLength, and each symbol should be from the alphabet: $alphabet.")
    guess = safeReadLine()
    isCorrect = isCorrectInput(guess, wordLength, alphabet)
  } while(!isCorrect)
  return guess
}

fun playGame(secret: String, wordLength: Int, maxAttemptsCount: Int) {
  var complete: Boolean
  var attempts = 0
  do {
    println("Please input your guess. It should be of length $wordLength.")
    val guess = safeReadLine()
    printRoundResults(secret, guess)
    complete = isComplete(secret, guess)
    attempts++
    if (isLost(complete, attempts, maxAttemptsCount)) {
      println("Sorry, you lost! :( My word is $secret")
      break
    } else if (isWon(complete, attempts, maxAttemptsCount)) {
      println("Congratulations! You guessed it!")
    }
  } while (!complete)
}

fun main() {
  val wordLength = 4
  val maxAttemptsCount = 3
  val secretExample = "ACEB"
  val alphabet = "ABCDEFGH"
  println(getGameRules(wordLength, maxAttemptsCount, secretExample))
  playGame(generateSecret(wordLength, alphabet), wordLength, maxAttemptsCount)
}