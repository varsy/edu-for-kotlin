package com.jetbrains.edu.cognifire.highlighting.undefinedidentifier

/**
 * Regex representation of the grammar.
 */
object GrammarRegex {
  private const val REGEX_DISJUNCTION = "|"

  private const val ARGUMENT = "(?:argument|arguments)"
  private const val TO = "to"
  private const val SAVE = "save"
  private const val RESULT = "result"
  private const val SEPARATOR = ","
  private const val ARTICLE = "(?:a|an|the)"
  private const val MODIFIED = "(?:multiplied|divided|incremented|decremented)"
  private const val BY = "by"
  private const val AND = "and"
  private const val VALUE = "(?:value|number)"
  private const val IF = "if"
  private const val ELSE = "(?:else|otherwise)"
  private const val THEN = "then"
  private const val LOOP = "(?:loop|for|iterate|go)"
  private const val OVER = "(?:over|through)"
  private const val ELEMENT = "(?:element|elements|item|items|index|indices)"
  private const val EACH = "(?:each|every|all)"
  private const val REPEAT = "(?:repeat|continue)"
  private const val UNTIL = "until"
  private const val WHILE = "while"
  private const val DO = "do"
  private const val EMPTY = "empty"
  private const val RANDOM = "random"
  private const val STRING_WORD = "string"
  private const val MUTABLE = "mutable"
  private const val CONST = "(?:constant|const)"
  private const val VAR = "(?:var|variable|$MUTABLE)"
  private const val VAL = "(?:val|$CONST)"
  private const val VARIABLE = "(?:$VAL|$VAR)"
  private const val SET = "(?:set|assign|give)"
  private const val CALLED = "(?:called|named)"
  private const val FUNCTION = "(?:fun|function)"
  private const val PRINT = "(?:print|prints|output|outputs|display|displays)"
  private const val STORE = "(?:store|stored)"
  private const val CALL = "(?:call|invoke|run|execute)"
  private const val IN = "(?:in|of)"
  private const val UP = "up"
  private const val CREATE = "(?:declare|create|initialize)"
  private const val BOOL = "\\b(?:true|false)\\b"
  private const val ADD = "(?:add|adds|append|appends)"
  private const val EQUAL = "(?:equal|equals)"
  private const val RETURN = "(?:return|returns)"
  private const val READ = "(?:read|reads)"
  private const val WITH = "with"
  private const val GET = "(?:get|gets|receive|receives|obtain|obtains)"
  private const val DATA_STRUCTURE = "(?:array|list|set|map|dictionary|hashmap|hashset|collection|iterable|sequence|queue|stack)"
  private const val NUMBER = "\\b(?:[0-9]+)\\b"
  private const val STRING = "\"[^\\r\\n\"]+\""
  private const val IDENTIFIER = "`([A-Za-z_][A-Za-z0-9_]*)`"
  private const val NO_CAPTURE_IDENTIFIER = "`[A-Za-z_][A-Za-z0-9_]*`"
  private const val CODE = "`[^`\\r\\n]+`"

  val value = listOf(
    RESULT,
    NUMBER,
    STRING,
    NO_CAPTURE_IDENTIFIER,
    BOOL,
    CODE
  ).joinToString("|", prefix = "(?:", postfix = ")")

  private val arbitraryText = listOf(
    ARTICLE,
    MODIFIED,
    BY,
    NO_CAPTURE_IDENTIFIER,
    AND,
    VALUE,
    IF,
    ELSE,
    THEN,
    LOOP,
    EACH,
    WHILE,
    DO,
    EMPTY,
    RANDOM,
    value,
    STRING_WORD,
    VARIABLE,
    CALLED,
    FUNCTION,
    PRINT,
    IN,
    REPEAT,
    UNTIL,
    ADD,
    EQUAL,
    RETURN,
    READ,
    WITH,
    GET,
    CONST,
    DATA_STRUCTURE,
    OVER
  ).joinToString("|")

  private val word = listOf(
    STRING_WORD,
    "(?:$RANDOM\\s+$STRING_WORD)",
    "(?:$EMPTY\\s+$STRING_WORD)",
    NUMBER,
    CODE,
  ).joinToString("|")

  val keyword = listOf(
    IF,
    ELSE,
    THEN,
    LOOP,
    EACH,
    WHILE,
    DO,
    EMPTY,
    RANDOM,
  ).joinToString("|", prefix = "(?i)\\b(", postfix = ")\\b").toRegex()

  val valueRegex = listOf(
    NUMBER,
    BOOL,
  ).joinToString("|", prefix = "(", postfix = ")").toRegex()

  val stringRegex = "($STRING)".toRegex()

  /**
   * Regex that matches variable storing. Example: ``Store 3 in the variable `foo` ``.
   */
  val storeVariable = ("(?i)($STORE)(?:\\s+$ARTICLE)?(?:\\s+$VALUE)?\\s+($value)(?:\\s+(?:$arbitraryText))*\\s+($IN)" +
                       "(?:\\s+$ARTICLE)?(?:\\s+$VARIABLE)?\\s+$IDENTIFIER").toRegex()

  /**
   * Regex that matches variable creation. Example: ``Create an empty string `foo` ``.
   */
  val createVariable = ("(?i)($CREATE)(?:\\s+$UP)?(?:\\s+$ARTICLE)?(?:\\s+($word))?(?:\\s+$VARIABLE)?" +
                        "(?:\\s+$CALLED)?(?:\\s+(?:$IDENTIFIER))").toRegex()

  /**
   * Regex that matches variable initialization. Example: "Set the variable `foo` to 3".
   */
  val setVariable = ("(?i)($SET)(?:\\s+$ARTICLE)?(?:\\s+$VARIABLE)?(?:\\s+$CALLED)?\\s+$IDENTIFIER(?:\\s+(?:$arbitraryText))*\\s+($TO)" +
                     "(?:\\s+$ARTICLE)?(?:\\s+$VALUE)?\\s+($value)").toRegex()

  /**
   * Regex that matches variable saving. Example: ``Save 3 to the variable `foo` ``.
   */
  val saveVariable = ("(?i)($SAVE)(?:\\s+$ARTICLE)?(?:\\s+$VALUE)?\\s+($value)(?:\\s+(?:$arbitraryText))*\\s+($TO)" +
                      "(?:\\s+$ARTICLE)?(?:\\s+$VARIABLE)?\\s+$IDENTIFIER").toRegex()

  /**
   * Regex that matches a function call. Example: "Call the function `foo` with 1 and 3".
   */
  val callFunction = (
    "(?i)($CALL)(?:\\s+$ARTICLE)?(?:\\s+$FUNCTION)?\\s+$IDENTIFIER" +
    "(?:(?:\\s+$WITH(?:\\s+$ARTICLE)?(?:\\s+$ARGUMENT)?)?\\s+($value(?:(?:\\s+$AND\\s+|\\s*$SEPARATOR\\s*)$value)*))?"
                     ).toRegex()

  /**
   * Regex that matches a loop expression. Example: ``For each `i` in the `array` ``.
   */
  val loopExpression = (
    "(?i)($LOOP)\\s+(?:$OVER\\s+)?(?:$EACH\\s+)?(?:$ELEMENT\\s+)?$IDENTIFIER\\s+($IN)\\s+" +
    "(?:$ARTICLE\\s+)?(?:$DATA_STRUCTURE\\s+)?$IDENTIFIER"
                       ).toRegex()

  /**
   * Regex that matches an isolated code in the text. Example: "`foo123`".
   */
  val isolatedCode = IDENTIFIER.toRegex()

  private fun String.disconnect() = drop(3).dropLast(1).split(REGEX_DISJUNCTION)
  fun getCallSynonyms() = CALL.disconnect()
  fun getFunctionSynonyms() = FUNCTION.disconnect()
  fun getCreateSynonyms() = CREATE.disconnect()
  fun getLoopSynonyms() = LOOP.disconnect()
  fun getOverSynonyms() = OVER.disconnect()
  fun getEachSynonyms() = EACH.disconnect()
  fun getElementSynonyms() = ELEMENT.disconnect()
  fun getDataStructureSynonyms() = DATA_STRUCTURE.disconnect()
}
