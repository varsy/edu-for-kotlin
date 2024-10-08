We have already leraned how to generate a list of random letters, but we need to work with strings.
To join a list of elements into a string,
you can use the [`joinToString`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/join-to-string.html)
function, passing the _separator_ — a character (or _string_)
that will be used to separate the elements — as an argument.:
```kotlin
// The separator is "; ", the resulting string is: "6; 6; 6; 6; 6"
List(5) { 6 }.joinToString("; ")
// The default separator is ", ", the resulting string is: "A, A, A, A, A"
List(5) { 'A' }.joinToString()
```