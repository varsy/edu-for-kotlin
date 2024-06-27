package com.jetbrains.edu.learning.theoryLookup

/**
 * Represents a Term that contains the original term from the task description text and its lemmatised (base form) version.
 */
data class Term(val original: String, val lemmatisedVersion: String)
