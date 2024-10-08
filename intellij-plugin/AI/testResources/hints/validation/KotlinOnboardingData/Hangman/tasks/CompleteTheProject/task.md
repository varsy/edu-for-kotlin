It's time to apply the implemented functions and finish the game!

### Task

Implement the `playGame` function,
which accepts a string `secret` - the secret of the current of the game,
`maxAttemptsCount` - the number of attempts that has the user in the game.
This function should implement the main game function.

<div class="hint" title="Click me to see the new signature of the playGame function">

The signature of the function is:
```kotlin
fun playGame(secret: String, maxAttemptsCount: Int): Unit
```
</div>

At the end of the game, the user should be informed about the results:
- if the user lost: `Sorry, you lost! My word is <secret>`
- if the user guessed the word: `Congratulations! You guessed it!`

**Note**: to avoid typos just copy the text from here and paste into your code.

Then, just call already implemented function `playGame` with two arguments: `secret`, `maxAttemptsCount` inside the `main` function and print it's output:
```kotlin
println(playGame(generateSecret(), maxAttemptsCount))
```

<div class="hint" title="Click me to see the game's example">

![The game's example](../../utils/src/main/resources/images/part1/hangman/game.gif "The game's example")

</div>