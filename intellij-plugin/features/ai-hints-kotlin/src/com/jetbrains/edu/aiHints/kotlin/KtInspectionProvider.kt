package com.jetbrains.edu.aiHints.kotlin

import com.jetbrains.edu.aiHints.core.InspectionProvider

class KtInspectionProvider : InspectionProvider {

  /**
   * @see <a href="https://github.com/jetbrains-academy/kotlin-course-template/blob/main/.idea/inspectionProfiles/README.md">Inspections on Kotlin Courses</a>
   */
  override val inspections: Set<String>
    get() = setOf(
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
}