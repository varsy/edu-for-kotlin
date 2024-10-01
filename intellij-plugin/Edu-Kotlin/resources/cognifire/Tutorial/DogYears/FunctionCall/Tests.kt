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
    fun testFunctionCall() {
        setUp()
        myFixture.configureByText("Task.kt", sourceText)

        val argument = getMethodCallArguments("prompt")?.firstOrNull()
        Assertions.assertNotNull(argument) {
            "Please, create a prompt block."
        }
        val values = Regex("`[^`]*`").findAll(argument!!).map { it.value }
        Assertions.assertNotNull(values.find { it == "`verifyHumanYearsInput`" }) {
            "Please, call the verifyHumanYearsInput function and wrap it in backticks (``)."
        }
        Assertions.assertEquals(2, values.filter { it == "`humanYears`" }.count()) {
            "Please, call the verifyHumanYearsInput function with humanYears argument and wrap it in backticks (``)."
        }
    }
}
