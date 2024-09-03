package jetbrains.course.tutorial.dog.years

import org.jetbrains.academy.jarvis.dsl.*

fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to a variable named `humanYears`.
        Call the function `verifyHumanYearsInput` with `humanYears`. 
        If the function returns true, set `dogYears` equal to `humanYears` multiplied by 7 and print the message "Your dog's age in dog years is: dogYears", 
        otherwise print the message "Oops! That doesn't look like a valid age.".
    """.trimIndent())
  code {
    val humanYears = readlnOrNull()?.toIntOrNull()
    if (humanYears != null && verifyHumanYearsInput(humanYears)) {
      val dogYears = humanYears * 7
      println("Your dog's age in dog years is: $dogYears")
    } else {
      println("Oops! That doesn't look like a valid age.")
    }
  }
}

fun verifyHumanYearsInput(humanYears: Int): Boolean {
  TODO("Not implemented yet")
}
