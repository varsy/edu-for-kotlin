In this task, we will implement the `safeUserInput` function to ensure the user only enters correct inputs.

### Task

Implement the `safeUserInput` function,
which accepts `wordLength` and `alphabet`
and handles incorrect user inputs.

<div class="hint" title="Click me to see the new signature of the safeUserInput function">

The signature of the function is:
```kotlin
fun safeUserInput(wordLength: Int, alphabet: String): String
```
</div>

- before reading the user input, print the requirements:

```text
Please input your guess. It should be of length <wordLength>, and each symbol should be from the alphabet: <alphabet>.
```

**Note**: to avoid typos, just copy the text from here and paste it into your code.

- to read the line of user input, use the `safeReadLine` function as earlier (or your own implementation of it);
- to check the correctness of the user's input, use the `isCorrectInput` function that was implemented in the previous step.

Here's an example of how the `safeUserInput` function works:

![The safeUserInput example](../../utils/src/main/resources/images/part1/warmup/safe_user_input.gif "The safeUserInput example")

To make the picture fit, additional line breaks were added.
You don't need to add them when solving the task.