### Advanced topic!
You can also combine natural language with code for your prompt!
Try to use this knowledge to implement `verifyHumanYearsInput` function.
<div class="hint" title="Example of code in a prompt">

```kotlin
fun verifyHumanYearsInput(humanYears: Int): Boolean {
  prompt("""
        return the following:
    """)
  {
    if(humanYears > 0) {
      true
    } else {
      false
    }
  }
}
```
</div>