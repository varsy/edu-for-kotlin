package com.jetbrains.edu.assistant.validation.accuracy

interface AccuracyCalculator<T> {
  fun calculateValidationAccuracy(manualRecords: List<T>, autoRecords: List<T>): T

  fun calculateOverallAccuracy(records: List<T>): T
}
