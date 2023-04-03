@file:JvmName("CopyUtils")

package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIncludeProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.tasks.Task
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
  //do not serialize or deserialize file contents, because we will restore their values after the deserialization
  mapper.addMixIn(FileContents::class.java, FileContentsMixin::class.java)
  mapper
}

@Suppress("unused") // used for serialization
@JsonDeserialize(builder = FileContentsBuilder::class)
@JsonIncludeProperties
private abstract class FileContentsMixin

@JsonPOJOBuilder(withPrefix = "")
private class FileContentsBuilder {
  @Suppress("unused") // used by json serializer
  private fun build(): FileContents = UndeterminedContents.EMPTY
}

fun <T : StudyItem> T.copy(): T {
  try {
    val jsonText = MAPPER.writeValueAsString(this)
    val copy = MAPPER.readValue(jsonText, javaClass)
    copy.init(parent, true)

    copyFileContents(this, copy)

    return copy
  }
  catch (e: JsonProcessingException) {
    LOG.error("Failed to create study item copy", e)
  }
  error("Failed to create study item copy")
}

private fun <T : StudyItem> copyFileContents(item1: T, item2: T) {
  if (item1 is Task) {
    copyFileContentsForTasks(item1, item2 as Task)
    return
  }
  if (item1 !is ItemContainer) return
  item2 as ItemContainer

  for (subItem1 in item1.items) {
    val subItem2 = item2.getItem(subItem1.name) ?: continue
    copyFileContents(subItem1, subItem2)
  }
}

private fun copyFileContentsForTasks(item1: Task, item2: Task) {
  for (taskFile1 in item1.taskFiles.values) {
    val taskFile2 = item2.getTaskFile(taskFile1.name)
    taskFile2?.contents = taskFile1.contents
  }
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
