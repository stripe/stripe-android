package com.stripe.android.identity.navigation

import android.content.Context
import androidx.annotation.IdRes
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_DESTINATION
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromFragment
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError

internal fun NavController.navigateToErrorScreenWithRequirementError(
    @IdRes fromFragment: Int,
    requirementError: VerificationPageDataRequirementError
) {
    navigateTo(
        ErrorDestination(
            errorTitle = requirementError.title ?: context.getString(R.string.error),
            errorContent = requirementError.body
                ?: context.getString(R.string.unexpected_error_try_again),
            backButtonText = requirementError.backButtonText ?: context.getString(R.string.go_back),
            backButtonDestination =
            if (requirementError.requirement.matchesFromFragment(fromFragment)) {
                fromFragment
            } else {
                UNEXPECTED_DESTINATION
            },
            shouldFail = false,
            cause = IllegalStateException("VerificationPageDataRequirementError: $requirementError")
        )
    )
}

internal fun NavController.navigateToErrorScreenWithDefaultValues(
    context: Context,
    cause: Throwable
) {
    navigateTo(
        ErrorDestination(
            errorTitle = context.getString(R.string.error),
            errorContent = context.getString(R.string.unexpected_error_try_again),
            backButtonDestination = R.id.consentFragment,
            backButtonText = context.getString(R.string.go_back),
            shouldFail = false,
            cause = cause
        )
    )
}

/**
 * Navigate to error fragment with failed reason. This would be the final destination of
 * verification flow, clicking back button would end the follow with
 * [IdentityVerificationSheet.VerificationFlowResult.Failed] with [failedReason].
 */
internal fun NavController.navigateToErrorScreenWithFailedReason(
    context: Context,
    failedReason: Throwable
) {
    navigateTo(
        ErrorDestination(
            errorTitle = context.getString(R.string.error),
            errorContent = context.getString(R.string.unexpected_error_try_again),
            backButtonText = context.getString(R.string.go_back),
            shouldFail = true,
            cause = failedReason
        )
    )
}
