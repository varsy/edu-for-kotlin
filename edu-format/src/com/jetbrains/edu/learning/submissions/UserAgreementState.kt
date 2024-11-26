package com.jetbrains.edu.learning.submissions

/**
 * Reflects the state of User Agreement acceptance
 * @property ACCEPTED user accepted the agreement and service can be provided
 * @property DECLINED user refused to accept the agreement and service can NOT be provided
 * @property TERMINATED user terminated the User Agreement, service can NOT be provided. 
 * We are obliged to store user data for at least 30 days after the agreement termination.
 * @property NOT_SHOWN user was never suggested to accept the User Agreement
 */
enum class UserAgreementState {
  ACCEPTED,
  DECLINED,
  TERMINATED,
  NOT_SHOWN;
}

fun UserAgreementState.isAccepted() = this == UserAgreementState.ACCEPTED

@Deprecated("")
fun UserAgreementState?.isSubmissionUploadAllowed(): Boolean = this == UserAgreementState.ACCEPTED

@Deprecated("")
fun UserAgreementState?.isSubmissionDownloadAllowed(): Boolean = when (this) {
  UserAgreementState.ACCEPTED, UserAgreementState.TERMINATED -> true
  else -> false
}

@Deprecated("")
fun UserAgreementState?.isSolutionSharingAllowed(): Boolean = this == UserAgreementState.ACCEPTED
