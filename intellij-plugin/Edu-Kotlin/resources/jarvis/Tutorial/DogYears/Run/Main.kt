package jetbrains.course.tutorial.dog.years

import org.jetbrains.academy.jarvis.dsl.description
import org.jetbrains.academy.jarvis.dsl.draft

fun calculateDogAgeInDogYears() {
  description("""
        Get the user input and save the result to a variable named `humanYears`.
        Call the function `verifyHumanYearsInput` with `humanYears`. 
        If the function returns true, set `dogYears` equal to `humanYears` multiplied by 7 and print the message "Your dog's age in dog years is: dogYears".
    """.trimIndent())
  draft {
    val humanYears = readlnOrNull()?.toIntOrNull()
    if (humanYears != null && verifyHumanYearsInput(humanYears)) {
      val dogYears = humanYears * 7
      println("Your dog's age in dog years is: $dogYears")
    } else {
      TODO("Specify what to do if the function `verifyHumanYearsInput` returns false")
    }
  }
}

fun verifyHumanYearsInput(humanYears: Int): Boolean {
  TODO("Not implemented yet")
}
