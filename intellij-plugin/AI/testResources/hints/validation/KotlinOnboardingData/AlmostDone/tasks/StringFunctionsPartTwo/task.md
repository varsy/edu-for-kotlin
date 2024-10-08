Let's implement the second filter!

### Task

Implement the `applySquaredFilter` function.
For the border symbol, please use the predefined variable `borderSymbol`, which stores `#`:
```kotlin
println(borderSymbol) // #
```

<div class="hint" title="Click me to see an example of the applySquaredFilter function's work">

Here's an example of the function's work:
<p>
    <img src="../../utils/src/main/resources/images/part1/almost.done/when_hint_2.png" alt="Example of the function's work" width="400"/>
</p>
</div>

To make the picture prettier, add a separator between the picture and the border.
For the separator, please use the predefined variable `separator`, which stores a space.
```kotlin
println("This is the value from the separator variable: $separator.") // This is the value from the separator variable:  .
```

**Note that the picture might not be a square, which means the width of different lines in the picture can vary.**
In other words, you need to pad the shorter lines with the `separator` to make the image square.
To get the width of the picture, you can use the predefined function `getPictureWidth`,
which returns the maximum length of all picture lines.

<div class="hint" title="Click me to see an example of the getPictureWidth function's work">

```kotlin
val pictureWidth = getPictureWidth(picture) // calculate the longest line in the picture and returns its length
```

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

<div class="hint" title="Click me to see several examples of how the applySquaredFilter function should work">

First example:
<img src="../../utils/src/main/resources/images/part1/almost.done/examples/squared/android.png" alt="Example of the function's work" width="400"/>

Second example:
<img src="../../utils/src/main/resources/images/part1/almost.done/examples/squared/monkey.png" alt="Example of the function's work" width="400"/>
</div>

<div class="hint" title="Click me to learn how to run the applySquaredFilter function with predefined pictures">

To check how your function works, you can run it in <code>main</code> by passing one of the predefined pictures:

```kotlin
fun main() {
  applyFilter(simba, "squared")   // an example with the simba picture
  applyFilter(monkey, "squared")  // an example with the monkey picture
  applyFilter(android, "squared") // an example with the android picture (this picture has different line lengths)
}
```
</div>


<div class="hint" title="Click me to learn the main idea of the algorithm">

You can use the `applyBordersFilter` function to add borders. Next,  
create two `StringBuilder` instances — one for the top part and the other for the bottom.
Proceed to poopulate them row by row.
</div>

<div class="hint" title="Click me to learn how to implement the getPictureWidth function on your own">

If you want, you can try to implement your own version of the `getPictureWidth` function:
split the picture using the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/lines.html">`lines`</a> function
and then use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/max-of-or-null.html">`maxOfOrNull`</a> function to calculate
the maximum length of all picture lines.
</div>
