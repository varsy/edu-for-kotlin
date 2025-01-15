package com.jetbrains.edu.cognifire.validation

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext

class StringToListOfIntDeserializer : JsonDeserializer<List<Int>>() {
  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext) =
    parser.text.let {
      if (it.isEmpty()) emptyList()
      else it.split(",").map { index -> index.trim().toInt() }
    }
}
