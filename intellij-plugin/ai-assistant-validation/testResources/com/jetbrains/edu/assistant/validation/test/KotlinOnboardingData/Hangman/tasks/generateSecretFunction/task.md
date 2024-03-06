The goal of this task is to generate a random word for the game.

### Task

Implement the `generateSecret` function, which generates a _random_ word from the `words` list.

<div class="hint" title="Click me to see the new signature of the generateSecret function">

The signature of the function is:
```kotlin
fun generateSecret(): String
```
</div>

This project already has a defined variable `words` with a list of words that are available for the game.
You just need to generate a _random_ element from this list.
To check which words are stored in the `words` list, you can print the elements:
```kotlin
println(words) // [AREA, BABY, BACK, BALL, BAND, BANK, BASE, BODY, BOOK, ... ]
```