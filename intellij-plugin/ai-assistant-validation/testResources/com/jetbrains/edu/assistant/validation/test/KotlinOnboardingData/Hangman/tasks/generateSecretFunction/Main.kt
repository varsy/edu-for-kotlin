package jetbrains.kotlin.course.hangman

// You will use this function later
fun getGameRules(wordLength: Int, maxAttemptsCount: Int) = "Welcome to the game!$newLineSymbol$newLineSymbol" +
                                                           "In this game, you need to guess the word made by the computer.$newLineSymbol" +
                                                           "The hidden word will appear as a sequence of underscores, one underscore means one letter.$newLineSymbol" +
                                                           "You have $maxAttemptsCount attempts to guess the word.$newLineSymbol" +
                                                           "All words are English words, consisting of $wordLength letters.$newLineSymbol" +
                                                           "Each attempt you should enter any one letter,$newLineSymbol" +
                                                           "if it is in the hidden word, all matches will be guessed.$newLineSymbol$newLineSymbol" +
                                                           "" +
                                                           "For example, if the word \"CAT\" was guessed, \"_ _ _\" will be displayed first, " +
                                                           "since the word has 3 letters.$newLineSymbol" +
                                                           "If you enter the letter A, you will see \"_ A _\" and so on.$newLineSymbol$newLineSymbol" +
                                                           "" +
                                                           "Good luck in the game!"

// You will use this function later
fun isWon(complete: Boolean, attempts: Int, maxAttemptsCount: Int) = complete && attempts <= maxAttemptsCount

// You will use this function later
fun isLost(complete: Boolean, attempts: Int, maxAttemptsCount: Int) = !complete && attempts > maxAttemptsCount

fun deleteSeparator(guess: String) = guess.replace(separator, "")

fun isComplete(secret: String, currentGuess: String) = secret == deleteSeparator(currentGuess)

fun generateNewUserWord(secret: String, guess: Char, currentUserWord: String): String {
  var newUserWord = ""
  for (i in secret.indices) {
    newUserWord += if (secret[i] == guess) {
      "${secret[i]}$separator"
    } else {
      "${currentUserWord[i * 2]}$separator"
    }
  }
  // Just newUserWord will be ok for the tests
  return newUserWord.removeSuffix(separator)
}

fun generateSecret() = words.random()

fun main() {
  // Uncomment this code on the last step of the game

  // println(getGameRules(wordLength, maxAttemptsCount))
  // playGame(generateSecret(), maxAttemptsCount)
}