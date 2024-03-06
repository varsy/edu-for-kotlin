package com.jetbrains.edu.kotlin.eduAssistant.courses

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.jetbrains.kotlin.idea.KotlinLanguage

val ijPluginCourse = course(language = KotlinLanguage.INSTANCE) {
  frameworkLesson {
    theoryTask(
      taskDescription = """
        [com.intellij.psi.util.PsiTreeUtil](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/psi/util/PsiTreeUtil.java)
        is a utility class in the IntelliJ Platform SDK that provides methods for navigating and querying the PSI tree of a project.

        The [com.intellij.psi.util.PsiTreeUtil.findChildrenOfType](https://github.com/JetBrains/intellij-community/blob/30cfa651ac2b9c50163368b56ee87ce1944543ec/platform/core-api/src/com/intellij/psi/util/PsiTreeUtil.java#L197C64-L197C64) method is used to find all children of a specified type within a given PSI element.
        It's particularly useful when you need to locate all instances of a particular element type, such as classes, methods, or variables, within a file or a code block.

        **Syntax:**
        ```java
        public static @Unmodifiable @NotNull <T extends PsiElement> Collection<T> findChildrenOfType(@Nullable PsiElement element, @NotNull Class<? extends T> aClass)
        ```

        **Parameters:**
        * **element**: The PSI element within which to search for children. This could be a PsiFile, a PsiClass, or any other PSI element.
        * **aClass**: The class type of the elements you are searching for. For example, PsiClass.class to find all classes.
      """.trimIndent(),
      taskDescriptionFormat = DescriptionFormat.MD
    ) {
      kotlinTaskFile(
        name = "main/kotlin/org/jetbrains/academy/plugin/course/dev/access/ElementsCounter.kt",
        visible = true,
        text = """
          package org.jetbrains.academy.plugin.course.dev.access
          
          import com.intellij.psi.PsiFile

          fun countKtClasses(psiFile: PsiFile): String {
            TODO("write your code here")
          }
        """.trimIndent()
      )
    }
    eduTask(
      taskDescription = """
        You need to implement a function `countKtClasses` which will 
        count number of kotlin classes declared in the given kotlin PSI file.
        
        <div class="hint" title="Which class should I use as aClass parameter?">
        
        Try to use `KtClass::class.java` value for aClass parameter for `findChildrenOfType`
        </div>
      """.trimIndent(),
      taskDescriptionFormat = DescriptionFormat.MD
    ) {
      kotlinTaskFile(
        name = "main/kotlin/org/jetbrains/academy/plugin/course/dev/access/ElementsCounter.kt",
        visible = true,
        text = """
          package org.jetbrains.academy.plugin.course.dev.access
          
          import com.intellij.psi.PsiFile
          import com.intellij.psi.util.PsiTreeUtil
          import org.jetbrains.kotlin.psi.KtClass

          fun countKtClasses(psiFile: PsiFile): String {
            PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java).toList().size
          }
        """.trimIndent()
      )
    }
  }
  frameworkLesson {
    theoryTask(
      taskDescription = """
        All operations with code like adding, deleting or moving methods, renaming variable, you name it, are implemented as PSI editions.
        Please skim the [Modify the PSI](https://plugins.jetbrains.com/docs/intellij/modifying-psi.html) documentation.

        Methods to edit PSI Element:
        * [`PsiElement.add()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L302) - to add a child to PSI Element into tree. To specify the place in children list use [`PsiElement.addBefore()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L312C14-L312C23) and [`PsiElement.addAfter()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L322)
        * [`PsiElement.delete()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L373) - to delete PSI Element from tree
        * [`PsiElement.replace()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L402) - to replace PSI Element in tree

        Also, you create PSI Elements by using:
        * [`PsiElement.copy()`](https://github.com/JetBrains/intellij-community/blob/19d9a1cc2d9c14df9c3bdee391e9e4795ac25cb9/platform/core-api/src/com/intellij/psi/PsiElement.java#L293) - to copy PSI Element subtree

        Moreover, there are some PisElement-specific methods like [`KtNamedFunction.setName()`](https://github.com/JetBrains/intellij-community/blob/bf3083ca66771e038eb1c64128b4e508f52acfad/platform/core-api/src/com/intellij/psi/PsiNamedElement.java#L39) as well as all `PsiNamedElement` inheritors.

        **IMPORTANT!**

        Every PSI modifications need to be wrapped in a [write action and in command](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/command/WriteCommandAction.java)

        ```kotlin
        WriteCommandAction.runWriteCommandAction(project) {
            // Here you can modify PSI Elements
        }
        ```
      """.trimIndent(),
      taskDescriptionFormat = DescriptionFormat.MD
    ) {
      kotlinTaskFile(
        name = "main/kotlin/org/jetbrains/academy/plugin/course/dev/edit/PsiElementsSorter.kt",
        visible = true,
        text = """
          package org.jetbrains.academy.plugin.course.dev.edit
          
          import org.jetbrains.kotlin.psi.KtClass

          fun sortMethods(ktClass: KtClass) {
              TODO("write your code here")
          }
        """.trimIndent()
      )
    }
    eduTask(
      taskDescription = """
        Sometimes, you want to make you code more searchable and sort methods by the alphabetical order.
        Now you can do it automatically, just several lines of code!
        Implement `sortMethods` function which takes PSI class and orders all inner methods in alphabetical order.
        
        
        <div class="hint" title="Where to start?">
        
        Get list of class methods, **copy** them and sort list of copies by name. Then use **replace** original methods one by one with copy which should stand in this position in sorted order.
        </div>
       """.trimIndent(),
      taskDescriptionFormat = DescriptionFormat.MD
    ) {
      kotlinTaskFile(
        name = "main/kotlin/org/jetbrains/academy/plugin/course/dev/edit/PsiElementsSorter.kt",
        visible = true,
        text = """
          package org.jetbrains.academy.plugin.course.dev.edit
          
          import com.intellij.openapi.command.WriteCommandAction
          import org.jetbrains.kotlin.psi.KtClass
          import org.jetbrains.kotlin.psi.KtNamedFunction
          
          fun sortMethods(ktClass: KtClass) {
              val project = ktClass.project
              WriteCommandAction.runWriteCommandAction(project) {
                  val methods = ktClass.declarations.filterIsInstance<KtNamedFunction>()
                  val sortedMethods = methods.sortedBy { it.name }.map { it.copy() as KtNamedFunction }
          
                  methods.zip(sortedMethods).forEach { (original, sortedCopy) ->
                      original.replace(sortedCopy)
                  }
              }
          }
        """.trimIndent()
      )
    }
  }
}