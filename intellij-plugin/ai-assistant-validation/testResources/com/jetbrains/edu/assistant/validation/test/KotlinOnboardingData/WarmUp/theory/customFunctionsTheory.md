On the previous task we used the already implemented _custom_ function `getGameRules`.
This lesson will share more details how you can create your own functions.

#### 1. What is a function?

When writing code, it is convenient to divide it into independent units –
[**functions**](https://kotlinlang.org/docs/functions.html), where each function performs a _specific_ action.
For example, it prints something on the screen or evaluates the value of some expression.

We have already seen examples of functions in this course - for example, `main`,
which we extended by new code, or _built-in_ functions such as `println` and `readlnOrNull`.
Their peculiarity is that they perform a certain sequence of actions (_always the same_).
Each function, like `println`, can be called by its name.

#### 2. How to create a new function

To create a function, you need to write the `fun` keyword and give it some name:
```kotlin
fun myName() {
    // Some code
}
```

#### 3. Function arguments

In addition, a function may have arguments.
These arguments are available in the function's body.
Arguments are declared in parentheses in the format `name: type`,
and each of the arguments can have a default value, for example:
```kotlin
// By default intVariable has the default value 10
fun myName(intVariable: Int = 10, strVariable: String) {
    // The arguments are available in the function:
    println("$strVariable: $intVariable")
}
```

#### 4. The returned value

The function can also return a value.
For that, you should specify the type of the return value
and return it using the `return` keyword:

```kotlin
fun myName(intVariable: Int, strVariable: String): Int {
    // The arguments are available in the function:
    println("$strVariable: $intVariable")
    return intVariable + 5
}
```