import org.jetbrains.academy.test.system.core.models.TestKotlinType
import org.jetbrains.academy.test.system.core.models.classes.TestClass
import org.jetbrains.academy.test.system.core.models.classes.findClassSafe
import org.jetbrains.academy.test.system.core.models.method.TestMethod
import org.jetbrains.academy.test.system.core.models.variable.TestVariable
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import util.throwInternalCourseError


class Test {
    companion object {
        private lateinit var mainClazz: Class<*>

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mainClazz = mainClass.findClassSafe() ?: throwInternalCourseError()
        }

        private val calculateDogAgeInDogYears = TestMethod(
            "calculateDogAgeInDogYears",
            TestKotlinType("Unit"),
            returnTypeJava = "Void",
        )

        private val verifyHumanYearsInput = TestMethod(
            "verifyHumanYearsInput",
            TestKotlinType("Boolean"),
            listOf(
                TestVariable("humanYears", "Int"),
            ),
        )

        private val mainClass = TestClass(
            classPackage = "jetbrains.course.tutorial.dog.years",
            customMethods = listOf(
                calculateDogAgeInDogYears,
                verifyHumanYearsInput,
            ),
        )
    }

    @Test
    fun testCalculateDogAgeInDogYears() {
        mainClass.checkMethod(mainClazz, calculateDogAgeInDogYears)
    }

    @Test
    fun testVerifyHumanYearsInput() {
        mainClass.checkMethod(mainClazz, verifyHumanYearsInput)
    }
}
