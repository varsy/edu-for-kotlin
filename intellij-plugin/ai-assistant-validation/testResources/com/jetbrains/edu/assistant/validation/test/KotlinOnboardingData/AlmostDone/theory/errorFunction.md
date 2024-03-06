The `when` expression is useful in handling edge cases.
For example, you can simply terminate the execution of the program
and inform the user about errors.
You can achieve this using the [`error`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/error.html) function:
```kotlin
fun checkNumber(x: Int): Int {
    return when (x) {
        0 -> { x + 5 }
        10 -> { x - 5 }
        else -> error("Unexpected number")
    }
}
```