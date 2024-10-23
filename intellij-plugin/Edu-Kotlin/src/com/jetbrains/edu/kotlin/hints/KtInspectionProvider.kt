package com.jetbrains.edu.kotlin.hints

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.jetbrains.edu.learning.hints.InspectionProvider
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtInspectionProvider : InspectionProvider {

  // https://github.com/jetbrains-academy/kotlin-course-template/blob/main/.idea/inspectionProfiles/README.md
  private val inspectionIds: Set<String> = setOf(
    "AddOperatorModifier",
    "AddVarianceModifier",
    "ImplicitThis",
    "ReplaceGetOrSet",
    "ReplaceManualRangeWithIndicesCalls",
    "ReplaceRangeToWithUntil",
    "ReplaceSizeCheckWithIsNotEmpty",
    "ReplaceSizeZeroCheckWithIsEmpty",
    "ReplaceToStringWithStringTemplate",
    "ReplaceCallWithBinaryOperator",
    "UnnecessaryVariable",
    "ConvertReferenceToLambda",
    "ConvertTwoComparisonsToRangeCheck",
    "IntroduceWhenSubject",
    "MayBeConstant",
    "MoveVariableDeclarationIntoWhen",
    "PublicApiImplicitType",
    "RedundantNullableReturnType",
    "RedundantSemicolon",
    "RedundantVisibilityModifier",
    "RedundantIf",
    "RemoveCurlyBracesFromTemplate",
    "RemoveRedundantQualifierName",
    "RemoveSingleExpressionStringTemplate",
    "RemoveToStringInStringTemplate",
    "CanBeVal",
    "LiftReturnOrAssignment",
    "SelfAssignment",
    "SimplifyBooleanWithConstants",
    "LoopToCallChain"
  )

  override fun getInspections() = KotlinLanguage.INSTANCE.getAllInspections().filter { it.id in inspectionIds }.toList()

  private fun Language.getAllInspections() = LocalInspectionEP.LOCAL_INSPECTION.extensions.filter { it.language == this.id }
    .mapNotNull { it.instantiateTool() as? LocalInspectionTool }
}
