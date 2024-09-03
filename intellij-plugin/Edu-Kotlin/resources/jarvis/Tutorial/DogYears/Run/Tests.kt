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
    fun testCodeBlock() {
        setUp()
        myFixture.configureByText("Task.kt", sourceText)

        Assertions.assertNotNull(getMethodCallArguments("code")) {
            "Please, run the function by clicking on the Run button."
        }
    }
}
