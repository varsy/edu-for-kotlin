package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course

private interface PathSegmentPredicate {
  fun check(pathSegment: String): Boolean
}

private typealias AttributesMutator = (CourseFileAttributesMutable) -> Unit

internal data class Rule(
  val specificity: Int,
  val setupAttributes: MutableList<AttributesMutator> = mutableListOf(),
  val pathModifier: (pathSegments: List<String>) -> List<String>?
)

class AttributesBuilderContext(private val specificity: Int = 0) {

  internal var rules: MutableList<Rule> = mutableListOf()
  internal val setupAttributes: MutableList<AttributesMutator> = mutableListOf()

  private fun accepts(arg: Any, pathSegment: String): Boolean = when (arg) {
    is String -> arg == pathSegment
    is Regex -> arg.containsMatchIn(pathSegment)
    is PathSegmentPredicate -> arg.check(pathSegment)
    else -> error("Rule arguments must be either String or Regex")
  }

  fun pred(predicate: (pathSegment: String) -> Boolean): Any = object : PathSegmentPredicate {
    override fun check(pathSegment: String): Boolean = predicate(pathSegment)
  }

  fun excludeFromArchive() {
    setupAttributes += { it.excludedFromArchive = true }
  }

  fun includeIntoArchive() {
    setupAttributes += { it.excludedFromArchive = false }
  }

  private fun addRule(args: Array<out Any>, subRules: AttributesBuilderContext.() -> Unit, checker: (arg: Any, pathSegments: List<String>) -> List<String>?) {
    val subContext = AttributesBuilderContext(specificity + 1)
    subContext.subRules()

    val baseRule = Rule(
      specificity,
      subContext.setupAttributes
    ) { pathSegments ->
      for (arg in args) {
        val list = checker(arg, pathSegments)
        if (list != null) return@Rule list
      }
      return@Rule null
    }

    rules += baseRule

    rules += subContext.rules.map { rule ->
      Rule(specificity + 1, rule.setupAttributes) { pathSegments ->
        val newPathSegments = baseRule.pathModifier(pathSegments) ?: return@Rule null
        rule.pathModifier(newPathSegments)
      }
    }
  }

  fun dir(vararg args: Any, subRules: AttributesBuilderContext.() -> Unit) = addRule(args, subRules) { arg, pathSegments ->
    val first = pathSegments.getOrNull(0) ?: return@addRule null
    if (accepts(arg, first)) pathSegments.drop(1) else null
  }

  fun name(vararg args: Any, subRules: AttributesBuilderContext.() -> Unit) = addRule(args, subRules) { arg, pathSegments ->
    val pathSegment = pathSegments.lastOrNull() ?: return@addRule null
    if (accepts(arg, pathSegment)) listOf() else null
  }

  fun path(vararg args: Any, subRules: AttributesBuilderContext.() -> Unit) = addRule(args, subRules) { arg, pathSegments ->
    for ((i, pathSegment) in pathSegments.withIndex()) {
      if (accepts(arg, pathSegment)) {
        return@addRule pathSegments.drop(i + 1)
      }
    }
    return@addRule null
  }

  fun extension(vararg args: Any, subRules: AttributesBuilderContext.() -> Unit) = addRule(args, subRules) { arg, pathSegments ->
    val pathSegment = pathSegments.lastOrNull() ?: return@addRule null
    val extension = pathSegment.substringAfterLast('.', "")
    if (accepts(arg, extension)) listOf() else null
  }
}

class AttributesEvaluator(base: AttributesEvaluator? = null, rulesBuilder: AttributesBuilderContext.() -> Unit) {

  private val rules: MutableList<Rule> = base?.rules?.toMutableList() ?: mutableListOf()
  private val baseContext: AttributesBuilderContext = AttributesBuilderContext(0)

  init {
    rulesBuilder(baseContext)
    this.rules += baseContext.rules
    this.rules += Rule(-1, baseContext.setupAttributes) { it }
  }

  fun attributesForFile(holder: CourseInfoHolder<out Course?>, file: VirtualFile): CourseFileAttributes {
    val attributes = CourseFileAttributesMutable()

    val relativePath = FileUtil.getRelativePath(holder.courseDir.path, file.path, VFS_SEPARATOR_CHAR) ?: return attributes.toImmutable()
    val pathSegments = relativePath.split('/')

    for (rule in rules.sortedBy { it.specificity }) {
      if (rule.pathModifier(pathSegments) != null) {
        for (setupAttributes in rule.setupAttributes) {
          setupAttributes(attributes)
        }
      }
    }

    return attributes.toImmutable()
  }
}