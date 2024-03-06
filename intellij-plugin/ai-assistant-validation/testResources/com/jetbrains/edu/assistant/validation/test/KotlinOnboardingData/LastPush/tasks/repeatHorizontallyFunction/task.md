Let' practice separating an algorithm into several functions. We will implement several additional functions
and then combine them into another function that applies a filter to the pattern.

### Task

Implement the `repeatHorizontally` function, which accepts a `pattern`, the number of repeats `n`, and `patternWidth`,
and then repeats the `pattern` `n` times horizontally.

<div class="hint" title="Click me to see the new signature of the getPatternHeight function">

The signature of the function is:
```kotlin
fun repeatHorizontally(pattern: String, n: Int, patternWidth: Int): String
```
</div>

**Note**, since the lines in the pattern can have different widths, you need to use the `fillPatternRow`
function to make all lines the same width.

In addition, the project already stores the `newLineSymbol` variable, which can be used to add new lines between newly generated picture lines, e.g.:
```kotlin
val line1 = "#######"
val line2 = "#######"

val line3 = "$line1$newLineSymbol$line2"
println(line3)
```

The result will be:
```text
#######
#######
```
</div>

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to see several examples of how the repeatHorizontally function should work">

The first example:
```kotlin
// Pattern: ○
// n = 1
// Result: ○
```

The second example:
```kotlin
// Pattern: ○
// n = 2
// Result: ○○
```

The third example:
```text
Pattern:
 X
/ \
\ /
 X
n = 1
Result:
 X
/ \
\ /
 X 
```

The fourth example:
```text
Pattern:
 X
/ \
\ /
 X
n = 2
Result:
 X  X 
/ \/ \
\ /\ /
 X  X 
```

</div>

<div class="hint" title="Click me to learn how to run the repeatHorizontally function with predefined patterns">

To check how your function works, you can run it in <code>main</code> by passing one of the predefined patterns:

```kotlin
fun main() {
    println("Pattern:")
    val n = 2
    println(rhombus)
    println("n = $n")
    println("Result:")
    println(repeatHorizontally(rhombus, n, getPatternWidth(rhombus)))
}
```
</div>