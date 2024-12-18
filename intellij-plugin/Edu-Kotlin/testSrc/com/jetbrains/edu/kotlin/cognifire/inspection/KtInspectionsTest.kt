package com.jetbrains.edu.kotlin.cognifire.inspection

import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class KtInspectionsTest : EduTestCase() {

  // TODO: Fix failing tests on Windows
  @Before
  fun checkIfWindows() {
    assumeTrue(!isWindows)
  }

  @Test
  fun testLiftReturnInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Do the following depending on what `arg` equals.",
        generatedCodeLine = "when (arg) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "If 0, return the string \"Zero\".",
        generatedCodeLine = "0 -> return \"Zero\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If 1, return the string \"One\".",
        generatedCodeLine = "1 -> return \"One\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Otherwise return the string \"Multiple\".",
        generatedCodeLine = "else -> return \"Multiple\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 4,
        promptLine = "Do the following depending on what `arg` equals.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Do the following depending on what `arg` equals.",
        generatedCodeLine = "return when (arg) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "If 0, return the string \"Zero\".",
        generatedCodeLine = "0 -> \"Zero\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If 1, return the string \"One\".",
        generatedCodeLine = "1 -> \"One\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Otherwise return the string \"Multiple\".",
        generatedCodeLine = "else -> \"Multiple\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 4,
        promptLine = "Do the following depending on what `arg` equals.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun foo(arg: Int): String",
        project,
        language
      )
    )
  }

  @Test
  fun testLiftAssignmentInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Declare the variable `result`.",
        generatedCodeLine = "var result: Int"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Depending on the conditions, we will initialise the `result` variable.",
        generatedCodeLine = "when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If `x` is greater than `y`, assign `result` - `x` plus `10`.",
        generatedCodeLine = "x > y -> result = x + 10"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "If `x` is equal to `y`, assign `result` - `x` plus `5`.",
        generatedCodeLine = "x == y -> result = x + 5"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "Otherwise assign `result` - `x` plus `1`.",
        generatedCodeLine = "else -> result = x + 1"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 5,
        promptLine = "Depending on the conditions, we will initialise the `result` variable.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Declare the variable `result`.",
        generatedCodeLine = "var result: Int"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Depending on the conditions, we will initialise the `result` variable.",
        generatedCodeLine = "result = when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If `x` is greater than `y`, assign `result` - `x` plus `10`.",
        generatedCodeLine = "x > y -> x + 10"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "If `x` is equal to `y`, assign `result` - `x` plus `5`.",
        generatedCodeLine = "x == y -> x + 5"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "Otherwise assign `result` - `x` plus `1`.",
        generatedCodeLine = "else -> x + 1"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 5,
        promptLine = "Depending on the conditions, we will initialise the `result` variable.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun calculate(x: Int, y: Int)",
        project,
        language
      )
    )
  }

  @Test
  fun testIntroduceWhenSubjectInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Return the following, under the following conditions.",
        generatedCodeLine = "return when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "If `obj` is a string, return \"string\".",
        generatedCodeLine = "obj is String -> \"string\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If `obj` is a integer, return \"int\".",
        generatedCodeLine = "obj is Int -> \"int\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Otherwise, return \"unknown\".",
        generatedCodeLine = "else -> \"unknown\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "Return the following, under the following conditions.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Return the following, under the following conditions.",
        generatedCodeLine = "return when (obj) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "If `obj` is a string, return \"string\".",
        generatedCodeLine = "is String -> \"string\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If `obj` is a integer, return \"int\".",
        generatedCodeLine = "is Int -> \"int\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Otherwise, return \"unknown\".",
        generatedCodeLine = "else -> \"unknown\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "Return the following, under the following conditions.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun test(obj: Any): String",
        project,
        language
      )
    )
  }

  @Test
  fun testIfThenToSafeAccessInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `a` is not equal to null.",
        generatedCodeLine = "if (a != null) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Call the `bar` function with `a`.",
        generatedCodeLine = "bar(a)"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 2,
        promptLine = "If `a` is not equal to null.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `a` is not equal to null.",
        generatedCodeLine = "a?.let { bar(it) }"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 0,
        promptLine = "Call the `bar` function with `a`.",
        generatedCodeLine = "a?.let { bar(it) }"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(promptToCodeTranslation, "fun foo(a: String?)", project, language)
    )
  }

  @Test
  fun testIfThenToElvisInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Set the value to the `bar` variable with the string \"hello\" if the `foo` variable is null.",
        generatedCodeLine = "val bar = if (foo == null) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "Set the value to the `bar` variable with the string \"hello\" if the `foo` variable is null.",
        generatedCodeLine = "\"hello\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 2,
        promptLine = "But assign the value in the bar variable to the foo variable if the foo variable is not equal to null.",
        generatedCodeLine = "} else {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 3,
        promptLine = "But assign the value in the bar variable to the foo variable if the foo variable is not equal to null.",
        generatedCodeLine = "foo"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 4,
        promptLine = "But assign the value in the bar variable to the foo variable if the foo variable is not equal to null.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Set the value to the `bar` variable with the string \"hello\" if the `foo` variable is null.",
        generatedCodeLine = "val bar = foo ?: \"hello\""
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 0,
        promptLine = "But assign the value in the bar variable to the foo variable if the foo variable is not equal to null.",
        generatedCodeLine = "val bar = foo ?: \"hello\""
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun test(foo: String?)",
        project,
        language
      )
    )
  }

  @Test
  fun testFoldInitializerAndIfToElvisInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Assign to variable `i` the variable `foo`.",
        generatedCodeLine = "var i = foo"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "If variable `i` is null, return variable `bar`.",
        generatedCodeLine = "if (i == null) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 2,
        promptLine = "If variable `i` is null, return variable `bar`.",
        generatedCodeLine = "return bar"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 3,
        promptLine = "If variable `i` is null, return variable `bar`.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 4,
        promptLine = "Return variable `i`.",
        generatedCodeLine = "return i"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Assign to variable `i` the variable `foo`.",
        generatedCodeLine = "var i = foo ?: return bar"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 0,
        promptLine = "If variable `i` is null, return variable `bar`.",
        generatedCodeLine = "var i = foo ?: return bar"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 1,
        promptLine = "Return variable `i`.",
        generatedCodeLine = "return i"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun test(foo: Int?, bar: Int): Int",
        project,
        language
      )
    )
  }

  @Test
  fun testJoinDeclarationAndAssignmentInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Define a string variable `x`.",
        generatedCodeLine = "val x: String"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Print a hello.",
        generatedCodeLine = "println(\"Hello\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "Set the system property `java.version` to the `x` variable and print it.",
        generatedCodeLine = "x = System.getProperty(\"java.version\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 3,
        promptLine = "Set the system property `java.version` to the `x` variable and print it.",
        generatedCodeLine = "println(x)"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "Define a string variable `x`.",
        generatedCodeLine = "val x: String=System.getProperty(\"java.version\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 0,
        promptLine = "Print a hello.",
        generatedCodeLine = "println(\"Hello\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 1,
        promptLine = "Set the system property `java.version` to the `x` variable and print it.",
        generatedCodeLine = "val x: String=System.getProperty(\"java.version\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "Set the system property `java.version` to the `x` variable and print it.",
        generatedCodeLine = "println(x)"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(promptToCodeTranslation, "fun foo()", project, language)
    )
  }

  @Test
  fun testCascadeIfInspection() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "if (id.isEmpty()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "} else if (!id.first().isIdentifierStart()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "} else if (!id.subSequence(1, id.length).all(Char::isIdentifierPart)) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 5,
        codeLineNumber = 5,
        promptLine = "Print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "print(\"Identifier should contain only letters and numbers\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 6,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "id.isEmpty() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 2,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 3,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 5,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "!id.first().isIdentifierStart() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 6,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 7,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 9,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "!id.subSequence(1, id.length).all(Char::isIdentifierPart) -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 5,
        codeLineNumber = 10,
        promptLine = "Print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "print(\"Identifier should contain only letters and numbers\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 11,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 13,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun checkIdentifier(id: String)",
        project,
        language
      )
    )
  }

  @Test
  fun testCascadeIfInspectionComplicatedCase() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "if (id.isEmpty()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "Print \"Identifier is empty2\".",
        generatedCodeLine = "print(\"Identifier is empty2\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 3,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 4,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "else if (!id.first().isIdentifierStart()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 5,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 6,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 5,
        codeLineNumber = 7,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "else if (!id.subSequence(1, id.length).all(Char::isIdentifierPart)) { }"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "id.isEmpty() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 2,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 3,
        promptLine = "Print \"Identifier is empty2\".",
        generatedCodeLine = "print(\"Identifier is empty2\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 4,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 6,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "!id.first().isIdentifierStart() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 7,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 8,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 5,
        codeLineNumber = 10,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "!id.subSequence(1, id.length).all(Char::isIdentifierPart) -> {}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 11,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun checkIdentifier(id: String)",
        project,
        language
      )
    )
  }

  @Test
  fun testCascadeIfInspectionWithElseBranch() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "if (id.isEmpty()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 1,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 2,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "} else if (!id.first().isIdentifierStart()) {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 3,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 4,
        promptLine = "Otherwise, print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "} else {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 5,
        promptLine = "Otherwise, print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "print(\"Identifier should contain only letters and numbers\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 6,
        promptLine = "If there is at least one character other than the first character in the `id` that is not an English letter or a number, do the following.",
        generatedCodeLine = "}"
      ),
    )
    val expectedPromptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "when {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "id.isEmpty() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 1,
        codeLineNumber = 2,
        promptLine = "Print \"Identifier is empty\".",
        generatedCodeLine = "print(\"Identifier is empty\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 3,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 5,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "!id.first().isIdentifierStart() -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 3,
        codeLineNumber = 6,
        promptLine = "Print \"Identifier should start with a letter\".",
        generatedCodeLine = "print(\"Identifier should start with a letter\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 2,
        codeLineNumber = 7,
        promptLine = "If the first character of the `id` is not an English letter, do the following.",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 9,
        promptLine = "Otherwise, print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "else -> {"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 10,
        promptLine = "Otherwise, print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "print(\"Identifier should contain only letters and numbers\")"
      ),
      GeneratedCodeLine(
        promptLineNumber = 4,
        codeLineNumber = 11,
        promptLine = "Otherwise, print \"Identifier should contain only letters and numbers\".",
        generatedCodeLine = "}"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 12,
        promptLine = "If `id` is empty, do the following.",
        generatedCodeLine = "}"
      ),
    )
    assertEquals(
      expectedPromptToCodeTranslation,
      InspectionProcessor.applyInspections(
        promptToCodeTranslation,
        "fun checkIdentifier(id: String)",
        project,
        language
      )
    )
  }

  companion object {
    private val language = KotlinLanguage.INSTANCE
  }
}
