package jetbrains.course.tutorial.dog.years

import org.jetbrains.academy.jarvis.dsl.description

fun calculateDogAgeInDogYears() {
  description("""
        Get the user input and save the result to a variable named `humanYears`.
        Call the function `verifyHumanYearsInput` with `humanYears`. 
        If the function returns true, set `dogYears` equal to `humanYears` multiplied by 7 and print the message "Your dog's age in dog years is: dogYears".
    """.trimIndent())
}

fun verifyHumanYearsInput(humanYears: Int): Boolean {
  TODO("Not implemented yet")
}
