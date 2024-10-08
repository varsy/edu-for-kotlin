Well, it looks like the game is ready! But what else is left to do?

### 1. Why do we need to inform the user about problems?

When writing programs, it is essential to consider _possible_ behavior
scenarios and adequately handle them. For example, in the current version of the game,
the user may enter a word that does not match the current game parameters
(the alphabet, word length, and so on). In such a case, it would be necessary to process
the situation and inform the user about the problem.
Doing so would allow the user to fix the problem and continue with the game.


### 2. How to simplify the `if` operator

Usually, to check several conditions, you need to use the `if` operator with multiple branches.
However, if the conditional is used inside a function with `return`,
the `else` word can be omitted:
```kotlin
fun myFunction(a: Int): String {
    if (a > 0) {
        return "Positive"
    } else if (a == 0) {
        return "Zero"
    } else {
        return "Negative"
    }
}
```
It is equal to:
```kotlin
fun myFunction(a: Int): String {
    if (a > 0) {
        return "Positive"
    }
    if (a == 0) {
        return "Zero"
    }
    return "Negative"
}
```