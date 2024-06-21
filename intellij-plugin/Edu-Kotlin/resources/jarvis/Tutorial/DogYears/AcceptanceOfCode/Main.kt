package jetbrains.course.tutorial.dog.years

fun calculateDogAgeInDogYears() {
  val humanYears = readlnOrNull()?.toIntOrNull()
  if (humanYears != null && verifyHumanYearsInput(humanYears)) {
    val dogYears = humanYears * 7
    println("Your dog's age in dog years is: $dogYears")
  } else {
    println("Oops! That doesn't look like a valid age.")
  }
}

fun verifyHumanYearsInput(humanYears: Int): Boolean {
  TODO("Not implemented yet")
}

