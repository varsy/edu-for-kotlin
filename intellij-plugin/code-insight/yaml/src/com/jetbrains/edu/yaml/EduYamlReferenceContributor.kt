package com.jetbrains.edu.yaml

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.EduPsiReferenceProvider
import com.jetbrains.edu.codeInsight.EduReferenceContributorBase
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.isTaskSpecialFile
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

class EduYamlReferenceContributor : EduReferenceContributorBase() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerEduReferenceProvider(EduYamlTaskFilePathReferenceProvider())
    registrar.registerEduReferenceProvider(ItemContainerContentReferenceProvider())
  }
}

class EduYamlTaskFilePathReferenceProvider : EduPsiReferenceProvider() {

  override val pattern: PsiElementPattern.Capture<YAMLScalar> = PSI_PATTERN

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val scalar = element as? YAMLScalar ?: return emptyArray()
    val taskDir = scalar.containingFile.originalFile.virtualFile?.parent ?: return emptyArray()
    return TaskFileReferenceSet(taskDir, scalar).allReferences
  }

  private class TaskFileReferenceSet(private val taskDir: VirtualFile, element: YAMLScalar) : FileReferenceSet(element) {
    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
      return Condition { item ->
        val virtualFile = item.virtualFile
        // Do not suggest files & directories that cannot be in course
        if (virtualFile.isTaskSpecialFile()) return@Condition false
        if (item.isDirectory) return@Condition true
        val filePaths = (element as YAMLScalar).collectFilePaths()
        val itemPath = VfsUtil.getRelativePath(virtualFile, taskDir) ?: return@Condition false
        itemPath !in filePaths
      }
    }

    override fun supportsExtendedCompletion(): Boolean = false

    private fun YAMLScalar.collectFilePaths(): List<String> {
      val sequence = parentOfType<YAMLSequence>() ?: return emptyList()
      return sequence.items.mapNotNull { item -> item.keysValues.find { it.keyText == NAME }?.valueText }
    }

    override fun createFileReference(range: TextRange?, index: Int, text: String?): FileReference? {
      return super.createFileReference(range, index, text)?.let(::TaskFileReference)
    }
  }

  private class TaskFileReference(fileReference: FileReference) : FileReference(fileReference) {
    override fun createLookupItem(candidate: PsiElement): Any? {
      return if (candidate is PsiDirectory) {
        LookupElementBuilder.createWithSmartPointer("${candidate.name}/", candidate)
          .withIcon(candidate.getIcon(0))
      } else null
    }
  }

  companion object {
    val PSI_PATTERN: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
      .inFileWithName(TASK_CONFIG)
      .withParent(
        keyValueWithName(NAME).inside(keyValueWithName(FILES))
      )
  }
}

class ItemContainerContentReferenceProvider : EduPsiReferenceProvider() {

  override val pattern: PsiElementPattern.Capture<YAMLScalar> = PSI_PATTERN

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val scalar = element as? YAMLScalar ?: return emptyArray()
    val itemContainerDir = scalar.containingFile.originalFile.virtualFile?.parent ?: return emptyArray()
    return ContentReferenceSet(itemContainerDir, scalar).allReferences
  }

  private class ContentReferenceSet(private val itemContainerDir: VirtualFile, element: YAMLScalar) : FileReferenceSet(element) {
    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
      return Condition { item ->
        if (!item.isDirectory) return@Condition false
        val containingFile = containingFile ?: return@Condition false
        // Suggest only direct children
        if (containingFile.parent?.findSubdirectory(item.name) != item) return@Condition false

        val virtualFile = item.virtualFile
        // Do not suggest files that cannot be in course
        if (excludeFromArchive(item.project, virtualFile)) return@Condition false

        val contentPaths = (element as YAMLScalar).collectContentPaths()
        val itemPath = VfsUtil.getRelativePath(virtualFile, itemContainerDir) ?: return@Condition false
        itemPath !in contentPaths
      }
    }

    override fun supportsExtendedCompletion(): Boolean = false

    private fun YAMLScalar.collectContentPaths(): List<String> {
      val sequence = parentOfType<YAMLSequence>() ?: return emptyList()
      return sequence.items.mapNotNull { (it.value as? YAMLScalar)?.textValue }
    }
  }

  companion object {
    val PSI_PATTERN: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
      .inFileWithName(COURSE_CONFIG, SECTION_CONFIG, LESSON_CONFIG)
      .withParent(
        psiElement<YAMLSequenceItem>().inside(keyValueWithName(CONTENT))
      )
  }}

private fun excludeFromArchive(project: Project, virtualFile: VirtualFile): Boolean {
  val course = StudyTaskManager.getInstance(project).course ?: return true
  val configurator = course.configurator ?: return true
  return configurator.excludeFromArchive(project, course, virtualFile)
}
