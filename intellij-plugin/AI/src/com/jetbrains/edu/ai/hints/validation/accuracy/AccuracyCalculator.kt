package com.jetbrains.edu.ai.hints.validation.accuracy

interface AccuracyCalculator<T> {
  fun calculateValidationAccuracy(manualRecords: List<T>, autoRecords: List<T>): T

  fun calculateOverallAccuracy(records: List<T>): T
}
