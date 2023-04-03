@file:JvmName("CopyUtils")

package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames
import com.jetbrains.edu.learning.json.mixins.StudyItemDeserializer
import com.jetbrains.edu.learning.serialization.SerializationUtils

private val LOG = logger<StudyItem>()

private val MAPPER: ObjectMapper by lazy {
  val mapper = ObjectMapper()
  mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
  mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
  mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

  val module = SimpleModule()
  module.addSerializer(StudyItem::class.java, StudyItemCopySerializer())
  module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())
  mapper.registerModule(module)
  mapper.addMixIn(FileContents::class.java, FileContentsMixin::class.java)
  mapper
}

@Suppress("unused") // used for serialization
@JsonDeserialize(builder = FileContentsBuilder::class)
private abstract class FileContentsMixin {
  lateinit var textualRepresentation: String
    @JsonProperty(JsonMixinNames.TEXT)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    get

  var isBinary: Boolean? = null
    @JsonProperty(JsonMixinNames.IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    get
}

@JsonPOJOBuilder(withPrefix = "")
private class FileContentsBuilder {

  @JsonProperty(JsonMixinNames.TEXT)
  lateinit var text: String
  @JsonProperty(JsonMixinNames.IS_BINARY)
  val isBinary: Boolean? = null

  @Suppress("unused") // used by json serializer
  private fun build(): FileContents = inMemoryFileContentsFromText(text, isBinary)
}

fun <T : StudyItem> T.copy(): T {
  try {
    val jsonText = MAPPER.writeValueAsString(this)
    val copy = MAPPER.readValue(jsonText, javaClass)
    copy.init(parent, true)
    return copy
  }
  catch (e: JsonProcessingException) {
    LOG.error("Failed to create study item copy", e)
  }
  error("Failed to create study item copy")
}

class StudyItemCopySerializer : JsonSerializer<StudyItem>() {
  override fun serialize(value: StudyItem, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    val javaType = provider.constructType(value::class.java)
    val beanDesc: BeanDescription = provider.config.introspect(javaType)
    val serializer: JsonSerializer<Any> =
      BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDesc,
                                                               provider.isEnabled(MapperFeature.USE_STATIC_TYPING))
    serializer.unwrappingSerializer(null).serialize(value, jgen, provider)
    if (value !is Course) {
      addItemType(value, jgen)
    }

    jgen.writeEndObject()
  }

  private fun addItemType(value: StudyItem, jgen: JsonGenerator) {
    val fieldName =
      if (value is Task) {
        SerializationUtils.Json.TASK_TYPE
      }
      else {
        SerializationUtils.Json.ITEM_TYPE
      }
    jgen.writeObjectField(fieldName, value.itemType)
  }
}
