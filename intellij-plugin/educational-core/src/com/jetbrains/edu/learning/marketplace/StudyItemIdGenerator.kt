package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.visitItems
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.reloadRemoteInfo
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import kotlin.random.Random

typealias DuplicateIdMap = Map<Int, List<StudyItem>>

@Service(Service.Level.PROJECT)
class StudyItemIdGenerator(private val project: Project) {

  /**
   * Generates ids for all study items in given [course] if they are not assigned yet (i.e. [StudyItem.id] equals 0)
   */
  @RequiresBlockingContext
  fun generateIdsIfNeeded(course: Course) {
    // Load `*-remote-info.yaml` files for each item to have up-to-date ids
    loadRemoteInfo(course)
    generateMissingIds(course)
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

  /**
   * Regenerate duplicate ids of [StudyItem]
   */
  suspend fun regenerateDuplicateIds(course: Course): List<StudyItem> {
    loadRemoteInfo(course)
    // Collect study items with duplicate ids
    val duplicateIds = collectItemsWithDuplicateIds(course)
    if (duplicateIds.isEmpty()) return emptyList()

    val remoteCourse = loadRemoteCourse(course)
    val itemsToFix = collectStudyItemsToGenerateNewIds(duplicateIds, remoteCourse)

    // Reset ids for items where we need new ids
    writeAction {
      for (item in itemsToFix) {
        item.id = 0
      }
    }

    // Avoid generating ids which already exist in the remote version to avoid unexpected behavior during course update
    val bannedIds = duplicateIds.keys + remoteCourse?.allItems.orEmpty().map { it.id }
    // TODO: convert other blocking parts into suspend function and drop `blockingContext`
    blockingContext {
      generateMissingIds(course, items = itemsToFix, bannedIds = bannedIds)
      // Dump info about new ids to `*-remote-info.yaml` files
      YamlFormatSynchronizer.saveRemoteInfo(course)
    }

    return itemsToFix
  }

  // cases for a particular duplicate id in the local course:
  // - no remote course - replace all ids with new ones
  // - 0 remote item with this id - replace all ids with new ones
  // - 1 remote item with this id - try to find the corresponding item in the local course, keep its id and replace others
  // - 2+ remote items with this id - replace all ids with new ones
  private fun collectStudyItemsToGenerateNewIds(duplicateIds: DuplicateIdMap, remoteCourse: EduCourse?): List<StudyItem> {
    if (remoteCourse == null) return duplicateIds.values.flatten()

    val remoteIdToItems = hashMapOf<Int, MutableList<StudyItem>>()
    remoteCourse.visitItems {
      val items = remoteIdToItems.getOrPut(it.id) { mutableListOf() }
      items += it
    }

    val result = mutableListOf<StudyItem>()
    for ((id, localItems) in duplicateIds) {
      val remoteItems = remoteIdToItems[id]
      val remoteItem = remoteItems?.singleOrNull()
      if (remoteItem == null) {
        result += localItems
      }
      else {
        // Try to find the same item on remote
        // It's important to preserve ids if possible not to break update of the corresponding item
        val (sameItems, otherItems) = localItems.partition { it.javaClass == remoteItem.javaClass && it.name == remoteItem.name }

        val mainLocalItem = sameItems.singleOrNull()

        result += if (mainLocalItem != null) otherItems else localItems
      }
    }

    return result
  }

  private suspend fun loadRemoteCourse(course: Course): EduCourse? {
    if (!(course is EduCourse && course.isMarketplaceRemote)) return null

    return withContext(Dispatchers.IO) {
      try {
        MarketplaceConnector.getInstance().loadCourse(course.id, DownloadCourseContext.OTHER)
      }
      catch (e: Exception) {
        LOG.warn(e)
        null
      }
    }
  }

  /**
   * Generates ids for all study items in given [course] if they are not assigned yet (i.e. [StudyItem.id] equals 0).
   *
   * [bannedIds] cannot be used as new ids even if there isn't any study item with such ids.
   * They are supposed to be used when we need to regenerate ids for some items, and we don't want to use old values
   */
  @RequiresBlockingContext
  private fun generateMissingIds(
    course: Course,
    items: List<StudyItem> = course.allItems,
    bannedIds: Set<Int> = emptySet()
  ) {
    // Generate missing ids
    val updates = hashMapOf<StudyItem, Int>()
    val usedIds = collectExistingIds(course)
    usedIds += bannedIds

    for (item in items) {
      if (item.id != 0) continue
      var newId: Int
      do {
        newId = generateNewId()
      }
      while (!usedIds.add(newId))

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
  }

  private fun loadRemoteInfo(course: Course) {
    course.visitItems { item ->
      item.reloadRemoteInfo(project)
    }
  }

  @VisibleForTesting
  fun generateNewId(): Int = Random.Default.nextInt(1, Int.MAX_VALUE)

  private fun collectExistingIds(course: Course): MutableSet<Int> {
    val ids = HashSet<Int>()
    course.visitItems { ids += it.id }
    return ids
  }

  companion object {
    private val LOG = logger<StudyItemIdGenerator>()

    fun getInstance(project: Project): StudyItemIdGenerator = project.service()

    private val Course.allItems: List<StudyItem>
      get() {
        val items = mutableListOf<StudyItem>()
        course.visitItems { items += it }
        return items
      }
  }
}
