It's time to apply the implemented functions and finish the game!

### Task

Replace the `safeReadLine` function inside the `playGame` function with the `safeUserInput` function implemented
in the previous step.
Since the `safeUserInput` function requires an `alphabet: String` argument, don't forget to update the signature of
the `playGame` function.

<div class="hint" title="Click me to see the new signature of the playGame function">

The signature of the function is:
```kotlin
fun playGame(secret: String, wordLength: Int, maxAttemptsCount: Int, alphabet: String): Unit
```
</div>

Finally, don't forget to use the `alphabet` argument inside the main function when you call the `playGame` function.

Good luck!

<div class="hint" title="Click me to see the final version of the game">

![The game's example](../../utils/src/main/resources/images/part1/warmup/game.gif "The game's example")

</div>