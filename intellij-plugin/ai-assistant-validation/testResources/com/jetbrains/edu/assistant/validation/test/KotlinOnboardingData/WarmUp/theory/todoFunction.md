Kotlin has a special [`TODO()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-t-o-d-o.html) function,  
which can be used as a temporary solution instead of implementing the function body:
```kotlin
fun myName(intVariable: Int): Int = TODO("Not implemented yet")
```

It can be useful when you need to define only _signatures_ of functions to build the main logic of the program,
but then implement them step-by-step.

If you use the `TODO` function, you **must** put the return type for this function directly:

```kotlin
fun correctFunction(intVariable: Int): Int = TODO("Not implemented yet") // CORRECT
fun incorrectFunction(intVariable: Int) = TODO("Not implemented yet") // INCORRECT
```