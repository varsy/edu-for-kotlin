Then, depending on the returned value, calculate the result and output the answer.

<div class="hint" title="Example of a prompt">

```kotlin
fun calculateDogAgeInDogYears() {
    prompt("""
        Get the user input and save the result to a variable named `humanYears`.
        Call the function `verifyHumanYearsInput` with `humanYears`. 
        If the function returns true, set `dogYears` equal to `humanYears` multiplied by 7 and print the message "Your dog's age in dog years is: dogYears".
    """.trimIndent())
}
```
</div>