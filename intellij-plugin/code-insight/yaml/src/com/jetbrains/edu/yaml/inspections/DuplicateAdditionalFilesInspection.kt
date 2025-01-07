package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.*
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.yaml.keyValueWithName
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class DuplicateAdditionalFilesInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlConfigSettings.COURSE_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : YamlPsiElementVisitor() {
    override fun visitScalar(scalar: YAMLScalar) {
      if (!pattern.accepts(scalar)) return

      val additionalFilesList = scalar.parent?.parent?.parent?.parent as? YAMLSequence ?: return
      val thisName = scalar.textValue
      val nameCount = additionalFilesList.children
        .count {
          val fileName = (it as? YAMLSequenceItem)
            ?.value
            ?.let { it as? YAMLMapping }
            ?.getKeyValueByKey(YamlMixinNames.NAME)
            ?.value
            ?.let { it as? YAMLScalar }
            ?.textValue

          fileName == thisName
        }

      if (nameCount >= 2) {
        holder.registerProblem(
          scalar,
          "Duplicate additional file",
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
      }
    }
  }

  private val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .withParent(keyValueWithName(YamlMixinNames.NAME))
    .withSuperParent(5, keyValueWithName(YamlMixinNames.ADDITIONAL_FILES))

}