Kotlin, like any other programming language,
already has many predefined (**built-in**) functions.
You may have noticed one of them in the previous task – `println`.
It allows you to display the text passed as an _argument_ in the console.
We need an argument in this case so that the function can perform
the _same_ action on _different_ data.

For example, if you want to display two words – `One` and `Two` – on different lines,
then in both cases you need to use the same [`println`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/println.html#println) function but _with different arguments_:
```kotlin
println("One")
println("Two")
```
The output is:
```text
One
Two
```

Kotlin also has another similar function - [`print`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/print.html#print).
The only difference from `println` is that it does not wrap text to a new line.
Thus, if we replace the `println` function from the previous example
with the `print` function, we get the following result:

```kotlin
print("One")
print("Two")
```
The output is:
```text
OneTwo
```

It is **important** to note that the text we want to print to the console
must be enclosed in _double quotes_.