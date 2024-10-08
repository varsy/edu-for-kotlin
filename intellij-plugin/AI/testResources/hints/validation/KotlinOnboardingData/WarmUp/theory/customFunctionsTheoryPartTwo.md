If a function can be expressed in [one statement](https://kotlinlang.org/docs/idioms.html#single-expression-functions) (one action in the code),
the `return` keyword, the type of the return value, and curly braces can be omitted. For example, consider the following code:
```kotlin
fun myName(intVariable: Int): Int {
    return intVariable + 5
}
```
It is equivalent to:
```kotlin
fun myName(intVariable: Int) = intVariable + 5
```

These functions usually help to make code shorter and write code in more Kotlin-like style.