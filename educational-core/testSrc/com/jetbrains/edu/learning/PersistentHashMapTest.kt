package com.jetbrains.edu.learning

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.PersistentHashMap
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Files
import java.nio.file.Paths

class PersistentHashMapTest : EduTestCase() {

  fun `test PersistentHashMap`() {
    val dir = Paths.get("/home/iliaposov/storage")
    FileUtil.deleteRecursively(dir)
    Files.createDirectories(dir)
    val file = dir.resolve("1")

    val map = PersistentHashMap<String, String>(
      file,
      EnumeratorStringDescriptor.INSTANCE,
      EnumeratorStringDescriptor.INSTANCE
    )

    map.put("key1", "value1")
    map.put("key2", "value2")
    map.put("key3", "value3")
    map.put("key4", "value4")

    assertEquals("value1", map["key1"])
    assertEquals("value2", map["key2"])
    assertEquals("value3", map["key3"])
    assertEquals("value4", map["key4"])

    map.dropMemoryCaches()
    map.close()

    val map2 = PersistentHashMap<String, String>(
      file,
      EnumeratorStringDescriptor.INSTANCE,
      EnumeratorStringDescriptor.INSTANCE
    )

    assertEquals("value1", map2["key1"])
    assertEquals("value2", map2["key2"])
    assertEquals("value3", map2["key3"])
    assertEquals("value4", map2["key4"])

    map.close()
  }

  fun `test PersistentHashMap memory`() {
    val dir = Paths.get("/home/iliaposov/storage")
    FileUtil.deleteRecursively(dir)
    Files.createDirectories(dir)
    val file = dir.resolve("1")

    val map = PersistentHashMap<String, ByteArray>(
      file,
      EnumeratorStringDescriptor.INSTANCE,
      object: DataExternalizer<ByteArray> {
        override fun save(out: DataOutput, value: ByteArray) {
          out.writeInt(value.size)
          out.write(value)
        }

        override fun read(`in`: DataInput): ByteArray {
          val size = `in`.readInt()
          val result = ByteArray(size)
          `in`.readFully(result, 0, size)
          return result
        }
      }
    )

    for (i in 1..10) {
      updateTotalMemory("before $i")
      val value1 = createByteArray(100_000_000)
      updateTotalMemory("array created $i")
      map.put("key$i", value1)
      updateTotalMemory("key$i written")
      map.force()
      updateTotalMemory("forced $i")
      println()
    }

    val v = map["key4"]
    println("getting value: ${v.size}")
    updateTotalMemory("got value key4")

    map.close()
    updateTotalMemory("closed")

    println(Runtime.getRuntime().totalMemory())
  }

  var totalMemoryBefore: Long = 0
  private fun updateTotalMemory(message: String) {
    System.gc()
    val total = Runtime.getRuntime().totalMemory()
    println("$message: total memoryUpdate = ${total - totalMemoryBefore}")
    totalMemoryBefore = total
  }

  private fun createByteArray(size: Int): ByteArray {
    val result = ByteArray(size)
    for (i in 0 until size) {
      result[i] = if (Math.random() > 0.5) 1 else 2
    }
    return result
  }

}