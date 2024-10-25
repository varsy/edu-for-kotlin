package com.jetbrains.edu.kotlin.hints

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.hints.InspectionProvider
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtInspectionProvider : InspectionProvider {

  /**
   * @see <a href="https://github.com/jetbrains-academy/kotlin-course-template/blob/main/.idea/inspectionProfiles/README.md">Inspections on Kotlin Courses</a>
   */
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

  override fun getInspections(): List<LocalInspectionTool> = LocalInspectionEP.LOCAL_INSPECTION.extensions
    .filter { it.language == KotlinLanguage.INSTANCE.id }
    .mapNotNull { it.instantiateTool().asSafely<LocalInspectionTool>() }
    .filter { it.id in inspectionIds }
}