### 1. What are multiline strings?

When working with long strings, the most convenient approach is to use so-called [multiline strings](https://kotlinlang.org/docs/coding-conventions.html#strings).
Unlike regular strings, they are enclosed in triple quotes:
```kotlin
// A regular string:
val regularString = "My text"

// A multiline string:
val multiRowString = """
    First line of the string\n
    Second line of the string
"""
```


### 2. The `trimIndent` and `trimMargin` functions

For convenient handling of such strings, Kotlin provides two functions: [`trimIndent`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/trim-indent.html) and [`trimMargin`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/trim-margin.html).
`trimIndent` detects the smallest common indentation of all the input lines,
removes it from every line, and also discards the first and the last lines
if they are blank.
`trimMargin` trims leading whitespace characters
followed by `marginPrefix` from every line of a source string and also removes
the first and last lines if they are blank.

For example, check out the following code:
```kotlin
fun main() {
    val multiRowString = """
    *First line of the string\n
    *Second line of the string
    """
    println("______REGULAR STRING_____")
    println(multiRowString)
    println("______TRIM INDENT_____")
    println(multiRowString.trimIndent())
    println("______TRIM MARGIN WITH *_____")
    println(multiRowString.trimMargin("*"))
}
```

The output will be:

```text
______REGULAR STRING_____

    *First line of the string\n
    *Second line of the string
    
______TRIM INDENT_____
*First line of the string\n
*Second line of the string
______TRIM MARGIN WITH *_____
First line of the string\n
Second line of the string
```

In the first case, the string will have extra indents and margins.
Also, each line in the string has the `*` symbol.
In the second case, the string looks better, but it still has an extra symbol in each line.
The last case looks like the best option.

Therefore, when working with multiline strings, such functions are a great help.