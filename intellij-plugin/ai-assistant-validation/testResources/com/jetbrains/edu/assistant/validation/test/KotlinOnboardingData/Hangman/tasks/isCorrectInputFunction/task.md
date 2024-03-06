In the next two tasks, we will implement functions to verify if the user input if it is correct.

### Task

Implement the `isCorrectInput` function, which accepts a string `userInput`
and checks if it is correct: 1) the length of `userInput` is 1, and 2) `userInput` is an English letter.
If `userInput` is correct, the function  returns `true`, and `false` otherwise.

<div class="hint" title="Click me to see the new signature of the getHiddenSecret function">

The signature of the function is:
```kotlin
fun isCorrectInput(userInput: String): Boolean
```
</div>

This function should have the following behavior:
- inform the user if the length of the input is incorrect:
  ```text
  The length of your guess should be 1! Try again!
  ```

- inform the user if the input is not an English letter:
  ```text
  You should input only English letters! Try again!
  ```

**Note**: to avoid typos, just copy the text from here and paste it into your code.

You can implement this function in any way you choose, but we _recommend_ looking into the [`isLetter`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/is-letter.html) built-in function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn how to check if the size of userInput is incorrect">

You can use `length` to get the number of letters in `userInput`:
```kotlin
val size = userInput.length
```
Then, you need to compare it with `1`.
</div>

<div class="Hint" title="Click me to learn more about the isLetter built-in function">

The built-in function <a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/is-letter.html'>`isLetter`</a> checks if the passed symbol
is an English letter, and it can be applied only to one letter at a time:
```kotlin
println("AB12"[0].isLetter()) // true, since `A` is an English letter
println("AB12"[1].isLetter()) // true, since `B` is an English letter
println("AB12"[2].isLetter()) // false, since `1` is NOT an English letter
println("AB12"[3].isLetter()) // false, since `2` is NOT an English letter
```
</div>