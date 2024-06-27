package com.jetbrains.edu.theoryLookup

import com.jetbrains.edu.learning.theoryLookup.Lemmatizer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.DefaultAsserter.assertEquals

@RunWith(Parameterized::class)
class LemmatizerTest(private val term: String, private val lemmatizedTerm: String) {

  companion object {
    @Parameterized.Parameters
    @JvmStatic
    fun data() = listOf(
      // inflected verbs: the verb should be converted to its base form
      arrayOf("Learning Kotlin", "learn kotlin"),
      // adjectives in superlative form
      arrayOf("easiest application", "easy application"),
      // verbs in progressive tense
      arrayOf("are writing Kotlin", "be write kotlin"),
      // gerund
      arrayOf("drinking coffee", "drink coffee"),
      // past participle and plural noun form
      arrayOf("contacted instructors", "contact instructor"),
      // one term in several places
      arrayOf("be writing Kotlin", "be write kotlin")
    )
  }

  @Test
  fun testLemmatization() {
    val text = "Learning Kotlin as a new developer can feel like building a castle while drinking coffee. " +
               "But don't worry, even the easiest application you build will equip you with its features. " +
               "Beginner's course will include real-world projects. You are writing Kotlin before you know it. " +
               "In the process, you will have contacted instructors and engaged in the developer's forum. " +
               "Not only will you be writing Kotlin code, but you'll find that often, Kotlin is writing your code for you."
    val terms = arrayListOf(
      "learn kotlin",
      "easy application",
      "be write kotlin",
      "drink coffee",
      "contact instructor"
    )
    val termToLemmaList = Lemmatizer(text, terms).getLemmatizedTermsList()
    assertEquals(
      "Expected lemmatized term did not match actual result. Original term: $term, lemmatized term: $lemmatizedTerm",
      lemmatizedTerm,
      termToLemmaList.find { it.original == term }?.lemmatisedVersion
    )
  }
}
