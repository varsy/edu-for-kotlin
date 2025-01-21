package com.jetbrains.edu.aiHints.python

import com.intellij.testFramework.utils.vfs.getPsiFile
import com.jetbrains.edu.aiHints.core.FunctionSignaturesProvider
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.python.PythonLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class PyFunctionSignaturesProviderTest(
  private val functionSignatures: List<FunctionSignature>,
  private val code: String,
) : EduActionTestCase() {
  override fun createCourse() {
    courseWithFiles {
      lesson("py_lesson") {
        eduTask("py_task") {
          taskFile("py_task.py", code)
        }
      }
    }
  }

  @Test
  fun `test getting function signatures`() {
    val psiFile = StudyTaskManager.getInstance(project).course
      ?.findTask("py_lesson", "py_task")
      ?.getTaskFile("py_task.py")
      ?.getVirtualFile(project)
      ?.getPsiFile(project) ?: error("PsiFile is not found")

    val actualResult = FunctionSignaturesProvider.getFunctionSignatures(psiFile, SignatureSource.MODEL_SOLUTION, PythonLanguage.INSTANCE)
    assertEquals(functionSignatures, actualResult)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = listOf(
      arrayOf(
        listOf(
          FunctionSignature("no_params", emptyList(), "None", SignatureSource.MODEL_SOLUTION, 2),
          FunctionSignature(
            "basic_types",
            listOf(
              FunctionParameter("num", "int"),
              FunctionParameter("text", "str"),
              FunctionParameter("flag", "bool")
            ),
            "float",
            SignatureSource.MODEL_SOLUTION,
            3
          )
        ), """
            def no_params() -> None:
                x = 42 + 3; z = 10
                y = x + z
                
            def basic_types(num: int, text: str, flag: bool) -> float:
                x = 21.0
                y = 42.0
                return x + y
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "complex_types",
            listOf(
              FunctionParameter("numbers", "List[int]"),
              FunctionParameter("mapping", "Dict[str, Any]"),
              FunctionParameter("optional", "Optional[str]")
            ),
            "Tuple[int, str]",
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def complex_types(numbers: List[int], mapping: Dict[str, Any], optional: Optional[str]) -> Tuple[int, str]:
                pass
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "with_defaults",
            listOf(
              FunctionParameter("x", "int"),
              FunctionParameter("y", "str"),
              FunctionParameter("z", "bool")
            ),
            "Any",
            SignatureSource.MODEL_SOLUTION,
            1
          ),
          FunctionSignature(
            "with_varargs",
            listOf(
              FunctionParameter("*args", "Any"),
              FunctionParameter("**kwargs", "Dict")
            ),
            "List",
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def with_defaults(x: int = 0, y: str = "", z: bool = True) -> Any:
                pass
                
            def with_varargs(*args: Any, **kwargs: Dict) -> List:
                pass
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "generic_function",
            listOf(
              FunctionParameter("data", "T"),
              FunctionParameter("transformer", "Callable[[T], S]")
            ),
            "S",
            SignatureSource.MODEL_SOLUTION,
            1
          ),
          FunctionSignature(
            "async_function",
            listOf(
              FunctionParameter("url", "str"),
              FunctionParameter("timeout", "float")
            ),
            "str",
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def generic_function[T, S](data: T, transformer: Callable[[T], S]) -> S:
                pass
                
            async def async_function(url: str, timeout: float) -> str:
                pass
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "union_types",
            listOf(
              FunctionParameter("value", "Union[int, str]"),
              FunctionParameter("container", "List[Union[int, str]]")
            ),
            "Union[List[int], Dict[str, Any]]",
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def union_types(value: Union[int, str], container: List[Union[int, str]]) -> Union[List[int], Dict[str, Any]]:
                pass
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "untyped_basic",
            listOf(
              FunctionParameter("a", null),
              FunctionParameter("b", null)
            ),
            null,
            SignatureSource.MODEL_SOLUTION,
            1
          ),
          FunctionSignature(
            "with_kwargs",
            listOf(
              FunctionParameter("x", null),
              FunctionParameter("**kwargs", null)
            ),
            null,
            SignatureSource.MODEL_SOLUTION,
            1
          ),
          FunctionSignature(
            "mixed_params",
            listOf(
              FunctionParameter("required", null),
              FunctionParameter("optional", null),
              FunctionParameter("*args", null),
              FunctionParameter("**kwargs", null)
            ),
            null,
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def untyped_basic(a, b):
                return a + b
                
            def with_kwargs(x, **kwargs):
                pass
                
            def mixed_params(required, optional="default", *args, **kwargs):
                pass
        """.trimIndent()
      ),

      arrayOf(
        listOf(
          FunctionSignature(
            "typed_func",
            listOf(
              FunctionParameter("x", "int"),
              FunctionParameter("y", "str")
            ),
            "bool",
            SignatureSource.MODEL_SOLUTION,
            1
          ),
          FunctionSignature(
            "untyped_func",
            listOf(
              FunctionParameter("x", null),
              FunctionParameter("y", null)
            ),
            null,
            SignatureSource.MODEL_SOLUTION,
            1
          )
        ), """
            def typed_func(x: int, y: str) -> bool:
                return len(y) == x
                
            def untyped_func(x, y):
                return x + y
        """.trimIndent()
      )
    )
  }
}