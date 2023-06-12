package com.jetbrains.edu.coursecreator

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.copyFileContents
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus

class CopyStudyItemTest : EduTestCase() {

  fun `test copy course`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
      section {
        lesson {
          eduTask { }
          choiceTask(isMultipleChoice = false, choiceOptions = choiceOptions)
        }
      }
    }.asRemote()

    checkItems(localCourse, localCourse.copy())
  }

  fun `test copy edu task`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("temp.py", "a = 1", true)
          taskFile("invisible.py", "a = 1", false)
        }
      }
    }
    val eduTask = localCourse.lessons[0].taskList[0]
    val taskCopy = eduTask.copy()

    copyFileContents(eduTask, taskCopy)

    assertTrue(eduTask.sameTo(taskCopy))
  }

  fun `test copy output task`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        outputTask { }
      }
    }
    val task = localCourse.lessons[0].taskList[0]
    val taskCopy = task.copy()
    assertTrue(task.sameTo(taskCopy))
  }

  fun `test copy theory task`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        theoryTask("theory", taskDescription = "my description", taskDescriptionFormat = DescriptionFormat.MD, stepId = 4) { }
      }
    }
    val task = localCourse.lessons[0].taskList[0]
    val taskCopy = task.copy()
    assertTrue(task.sameTo(taskCopy))
  }

  fun `test copy choice task`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(isMultipleChoice = false, choiceOptions = choiceOptions)
      }
    }
    val task = localCourse.lessons[0].taskList[0]
    val taskCopy = task.copy()
    assertTrue(task.sameTo(taskCopy))
  }

  private fun checkItems(eduCourse: EduCourse, courseCopy: EduCourse) {
    assertTrue(eduCourse.courseInfoSameTo(courseCopy))
    assertTrue(eduCourse.vendorsSameTo(courseCopy))
    assertTrue(eduCourse.additionalFilesSameTo(courseCopy))
    assertTrue(eduCourse.pluginDependenciesSameTo(courseCopy))
    assertTrue(eduCourse.itemsSameTo(courseCopy))
  }

  private fun EduCourse.itemsSameTo(courseCopy: EduCourse): Boolean {
    items.sameTo(courseCopy.items) { item, itemCopy ->
      item.id == itemCopy.id &&
      item.index == itemCopy.index &&
      item.name == itemCopy.name &&
      item.itemType == itemCopy.itemType &&
      item.updateDate == itemCopy.updateDate
    }

    allTasks.sameTo(courseCopy.allTasks) { task, taskCopy ->
      task.sameTo(taskCopy)
    }
    return true
  }

  private fun Task.sameTo(taskCopy: Task): Boolean {
    if (!taskFilesSameTo(taskCopy)) return false
    if (name != taskCopy.name ||
        status != taskCopy.status ||
        feedbackLink != taskCopy.feedbackLink ||
        feedback != taskCopy.feedback ||
        solutionHidden != taskCopy.solutionHidden ||
        descriptionFormat != taskCopy.descriptionFormat ||
        descriptionText != taskCopy.descriptionText ||
        itemType != taskCopy.itemType) return false
    return true
  }

  private fun EduCourse.pluginDependenciesSameTo(courseCopy: EduCourse): Boolean {
    pluginDependencies.sameTo(courseCopy.pluginDependencies) { pluginDependency, pluginDependencyCopy ->
      pluginDependency.stringId == pluginDependencyCopy.stringId &&
      pluginDependency.displayName == pluginDependencyCopy.displayName &&
      pluginDependency.maxVersion == pluginDependencyCopy.maxVersion &&
      pluginDependency.minVersion == pluginDependencyCopy.minVersion
    }
    return true
  }

  private fun EduCourse.additionalFilesSameTo(courseCopy: EduCourse): Boolean {
    additionalFiles.sameTo(courseCopy.additionalFiles) { additionalFile, additionalFileCopy ->
      additionalFile.sameTo(additionalFileCopy)
    }
    return true
  }

  private fun EduCourse.vendorsSameTo(courseCopy: EduCourse): Boolean {
    val courseCopyVendor = courseCopy.vendor
    val courseVendor = vendor
    if (courseVendor == null) {
      return courseCopyVendor == null
    }
    if (courseCopyVendor == null) {
      return false
    }
    return courseVendor.name == courseCopyVendor.name &&
           courseVendor.email == courseCopyVendor.email &&
           courseVendor.url == courseCopyVendor.url
  }

  private fun EduCourse.courseInfoSameTo(courseCopy: EduCourse): Boolean {
    return name == courseCopy.name &&
           index == courseCopy.index &&
           id == courseCopy.id &&
           updateDate == courseCopy.updateDate &&
           createDate == courseCopy.createDate &&
           description == courseCopy.description &&
           humanLanguage == courseCopy.humanLanguage &&
           languageId == courseCopy.languageId &&
           environment == courseCopy.environment &&
           courseMode == courseCopy.courseMode &&
           formatVersion == courseCopy.formatVersion &&
           solutionsHidden == courseCopy.solutionsHidden &&
           visibility == courseCopy.visibility &&
           isMarketplace == courseCopy.isMarketplace &&
           marketplaceCourseVersion == courseCopy.marketplaceCourseVersion &&
           organization == courseCopy.organization &&
           isMarketplacePrivate == courseCopy.isMarketplacePrivate &&
           feedbackLink == courseCopy.feedbackLink
  }

  private fun Task.taskFilesSameTo(taskCopy: Task): Boolean {
    val taskCopyFiles = taskCopy.taskFiles
    if (taskFiles.size != taskCopyFiles.size) return false

    for ((path, taskFile) in taskFiles) {
      val taskCopyFile = taskCopyFiles[path] ?: return false
      if (!taskFile.sameTo(taskCopyFile)) return false
    }
    return true
  }

  private fun TaskFile.sameTo(otherTaskFile: TaskFile): Boolean {
    if (this === otherTaskFile) return true

    val answerPlaceholdersEqual = answerPlaceholders.size == otherTaskFile.answerPlaceholders.size &&
                                  answerPlaceholders.zip(otherTaskFile.answerPlaceholders).all { it.first.sameTo(it.second) }

    return name == otherTaskFile.name &&
           text == otherTaskFile.text &&
           isVisible == otherTaskFile.isVisible &&
           answerPlaceholdersEqual
  }

  private fun EduFile.sameTo(otherTaskFile: EduFile): Boolean {
    if (this === otherTaskFile) return true

    return name == otherTaskFile.name &&
           text == otherTaskFile.text &&
           isVisible == otherTaskFile.isVisible
  }

  private fun AnswerPlaceholder.sameTo(otherPlaceholder: AnswerPlaceholder): Boolean {
    if (this === otherPlaceholder) return true

    val placeholderDependencyEqual = placeholderDependency?.sameTo(otherPlaceholder.placeholderDependency)
                                     ?: (otherPlaceholder.placeholderDependency == null)

    return offset == otherPlaceholder.offset &&
           length == otherPlaceholder.length &&
           index == otherPlaceholder.index &&
           placeholderText == otherPlaceholder.placeholderText &&
           placeholderDependencyEqual
  }

  private fun AnswerPlaceholderDependency.sameTo(otherDependency: AnswerPlaceholderDependency?): Boolean {
    if (this === otherDependency) return true
    if (otherDependency == null) return false

    return isVisible == otherDependency.isVisible &&
           fileName == otherDependency.fileName &&
           lessonName == otherDependency.lessonName &&
           placeholderIndex == otherDependency.placeholderIndex &&
           sectionName == otherDependency.sectionName
  }

  private inline fun <T> List<T>.sameTo(other: List<T>, equals: (T, T) -> Boolean): Boolean {
    if (size != other.size) return false
    return zip(other).all { (o1, o2) -> equals(o1, o2) }
  }
}