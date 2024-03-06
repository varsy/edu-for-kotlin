package com.jetbrains.edu.assistant.validation.util

fun <T> calculateCriterionResultAccuracy(
  record: List<T>,
  field: T.() -> String,
  compareAction: (String) -> Boolean
) = calculateCriterionAccuracy(null, record, field) { f, _ -> compareAction(f) }

fun <T> calculateCriterionAccuracy(
  manualRecords: List<T>?,
  autoRecords: List<T>,
  field: T.() -> String,
  compareAction: (String, String?) -> Boolean
): String {
  var matchCount = 0
  var totalCount = 0
  for (i in autoRecords.indices) {
    if (autoRecords[i].field().isNotBlank()) {
      if (compareAction(autoRecords[i].field(), manualRecords?.get(i)?.field())) {
        matchCount++
      }
      totalCount++
    }
  }
  return "${matchCount.toDouble() / totalCount * PERCENT_DENOMINATOR}"
}

fun areSameCriteria(first: String, second: String, firstOption: String = YES_KEYWORD, secondOption: String = NO_KEYWORD) =
  (first.startsWith(firstOption, ignoreCase = true) && second.startsWith(firstOption, ignoreCase = true)) ||
  (first.startsWith(secondOption, ignoreCase = true) && second.startsWith(secondOption, ignoreCase = true))

fun isCorrectAnswer(answer: String, option: String = YES_KEYWORD) = answer.startsWith(option, ignoreCase = true)


const val PERCENT_DENOMINATOR = 100
const val ACCURACY_KEYWORD = "Accuracy:"
const val YES_KEYWORD = "yes"
const val NO_KEYWORD = "no"
const val BOH_KEYWORD = "BOH"
const val HLD_KEYWORD = "HLD"
const val NEW_KEYWORD = "new"
const val CHANGED_KEYWORD = "changed"
const val DELETED_KEYWORD = "deleted"
const val ALLOWABLE_TOTAL_LENGTH_OF_CHANGES = 4
