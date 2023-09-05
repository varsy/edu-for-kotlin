package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PLUGIN_URL
import com.jetbrains.edu.learning.messages.EduCoreBundle

const val HYPERSKILL_URL_PROPERTY = "Hyperskill URL"
const val HYPERSKILL_DEFAULT_URL = "https://hyperskill.org/"
const val HYPERSKILL_DEFAULT_HOST = "hyperskill.org"
const val JBA_HELP = "${MARKETPLACE_PLUGIN_URL}/10081-jetbrains-academy/docs/jetbrains-academy-on-hyperskill.html"
const val HYPERSKILL_COMMENT_ANCHOR = "#comment"
const val HYPERSKILL_SOLUTIONS_ANCHOR = "#solutions"

val HYPERSKILL_PROFILE_PATH = "${HYPERSKILL_URL}profile/"

val SELECT_PROJECT = EduCoreBundle.message("hyperskill.select.project", HYPERSKILL_PROJECTS_URL, EduNames.JBA)

val HYPERSKILL_ENVIRONMENTS = mapOf("android" to EduNames.ANDROID, "unittest" to EduNames.UNITTEST)

val HYPERSKILL_URL: String
  get() = PropertiesComponent.getInstance().getValue(HYPERSKILL_URL_PROPERTY, HYPERSKILL_DEFAULT_URL)

/**
 * Legacy code: this name used to be a project name generated by code problem (or code challenge) opening
 * Now project name for this purpose is [com.jetbrains.edu.learning.stepik.hyperskill.HyperskillNamesKt.getProblemsProjectName]
 * */
fun getLegacyProblemsProjectName(language: String) = "${language.capitalize()} Code Challenges"

fun getProblemsProjectName(language: String) = "${language.capitalize()} Problems"
