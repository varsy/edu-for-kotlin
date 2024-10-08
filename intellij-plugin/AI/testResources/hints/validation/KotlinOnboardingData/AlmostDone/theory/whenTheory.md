### 1. What is the `when` expression?

We are already familiar with the `if` expression; however,
it may not be convenient in all cases:
for example, when we need to check a large number of values.
In such cases, you can use the [`when`](https://kotlinlang.org/docs/control-flow.html#when-expression) expression.

For example, take a look at the following code:
```kotlin
fun checkNumber(x: Int) {
    if (x > 0) {
        println("A positive number")
    } else if (x < 0) {
        println("A negative number")
    } else {
        println("The zero number")
    }
}
```
The above code can be replaced with the following:
```kotlin
fun checkNumber(x: Int) {
    when {
        x > 0 -> println("A positive number")
        x < 0 -> println("A negative number")
        else -> println("The zero number")
    }
}
```
Now it is shorter and easier to read.

### 2. Using the `when` expression: checking the variable's value

In addition, you can specify a variable that you want to compare
with some values and just list these values below. For example:

```kotlin
fun checkNumber(x: Int): Int {
    return if (x == 0) {
        x + 5
    } else if (x == 10) {
        x - 5
    } else {
        x / 10
    }
}
```
It can be replaced with:
```kotlin
fun checkNumber(x: Int): Int {
    return when (x) {
        0 -> { x + 5 }
        10 -> { x - 5 }
        else -> { x / 10 }
    }
}
```