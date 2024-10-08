When working with multiline strings, you can break them into lines and handle each one separately.
This can be done using the [`lines`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/lines.html) function:
```kotlin
"""First line
Second line
""".lines()
```
The string will be converted into a list with two strings: `First line` and `Second line`.

This function is better than `split`, since it considers
newline symbols from various operating systems under the hood:

```kotlin
"""First line
Second line
""".lines() // works on all OSs
```
VS
```kotlin
"""First line
Second line
""".split("\n") // does not work on Windows
```