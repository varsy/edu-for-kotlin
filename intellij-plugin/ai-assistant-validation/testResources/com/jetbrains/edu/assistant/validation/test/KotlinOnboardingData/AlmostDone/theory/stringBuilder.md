To create strings, it is best to use a special mechanism, [`StringBuilder`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-string-builder/),
which allows you to construct a long string from parts.
It also allows you to avoid copying the whole string each time it is updated, for example:
```kotlin
var myString = "It"
myString += "is"
myString += "string"
```
In this case, we will create a string with the text `It`, then recreate it, but with additional text `is`, and so on.
This impacts the program's execution time as well as the amount of computer resources it
consumes.

`StringBuilder` is designed to avoid this kind of copying.
First, you should create a string, then apply the `append` function to update it,
and finally, use the `toString` function to return the resulting string:
```kotlin
val sb = StringBuilder()
sb.append("It")
sb.append("is")
sb.append("string")
sb.toString()
```