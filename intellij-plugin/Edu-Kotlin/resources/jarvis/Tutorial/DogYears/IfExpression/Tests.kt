import org.jetbrains.academy.test.system.kotlin.test.BaseIjTestClass
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File


class Test : BaseIjTestClass() {
    companion object {

        private lateinit var sourceText: String

        @JvmStatic
        @BeforeAll
        fun initialize() {
            val taskDirectoryPath = System.getProperty("user.dir")
            val sourceCodeFile =
                File("$taskDirectoryPath/src/jetbrains/course/tutorial/dog/years/Main.kt")
            sourceText = sourceCodeFile.readText()
        }
    }

    @Test
    fun testIfExpression() {
        setUp()
        myFixture.configureByText("Task.kt", sourceText)

        val argument = getMethodCallArguments("prompt")?.firstOrNull()
        Assertions.assertNotNull(argument) {
            "Please, create a prompt block."
        }
        Assertions.assertTrue(argument?.contains("if", true) == true) {
            "You can use the word 'if' when you want to indicate what should happen depending on the returned value."
        }
        val values = Regex("\"(.+?)\"").findAll(argument!!).map { it.value }
        Assertions.assertNotNull(values.find { it.startsWith("\"Your dog's age in dog years is:") }) {
            "Please, write that outputs the message \"Your dog's age in dog years is: dogYears\"."
        }
    }
}
