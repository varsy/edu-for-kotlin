package com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger

data class JsEventData(val term: String, val x: Double, val y: Double)

val LOG: Logger = Logger.getInstance(JsEventData::class.java)

fun parseData(data: String): JsEventData? =
  try {
    Gson().fromJson(data, JsEventData::class.java)
  } catch (e: JsonSyntaxException) {
    LOG.error("Failed to parse js event data $data", e)
    null
  }
