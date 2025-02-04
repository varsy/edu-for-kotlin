package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.visitItems
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.reloadRemoteInfo
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.VisibleForTesting
import kotlin.random.Random

typealias DuplicateIdMap = Map<Int, List<StudyItem>>

@Service(Service.Level.PROJECT)
class StudyItemIdGenerator(private val project: Project) {

  /**
   * Generates ids for all study items in given [course] if they are not assigned yet (i.e. [StudyItem.id] equals 0)
   */
  fun generateIdsIfNeeded(course: Course) {
    // Load `*-remote-info.yaml` files for each item to have up-to-date ids
    course.visitItems { item ->
      item.reloadRemoteInfo(project)
    }

    // Generate missing ids
    val updates = hashMapOf<StudyItem, Int>()
    val existingIds = collectExistingIds(course)
    course.visitItems { item ->
      if (item.id != 0) return@visitItems
      var newId: Int
      do {
        newId = generateNewId()
      }
      while (!existingIds.add(newId))

      updates[item] = newId
    }

    // Save new ids to the corresponding study items
    invokeAndWaitIfNeeded {
      runWriteAction {
        for ((item, id) in updates) {
          item.id = id
        }
      }
    }

    // Dump info about new ids to `*-remote-info.yaml` files
    YamlFormatSynchronizer.saveRemoteInfo(course)
  }

  fun collectItemsWithDuplicateIds(course: Course): DuplicateIdMap {
    val idToItems = hashMapOf<Int, MutableList<StudyItem>>()
    course.visitItems { item ->
      // item doesn't have remote id
      if (item.id == 0) return@visitItems

      val items = idToItems.getOrPut(item.id) { mutableListOf() }
      items += item
    }

    return idToItems.filterValues { it.size > 1 }
  }

  @VisibleForTesting
  fun generateNewId(): Int = Random.Default.nextInt(1, Int.MAX_VALUE)

  private fun collectExistingIds(course: Course): MutableSet<Int> {
    val ids = HashSet<Int>()
    course.visitItems { ids += it.id }
    return ids
  }

  companion object {
    fun getInstance(project: Project): StudyItemIdGenerator = project.service()
  }
}
