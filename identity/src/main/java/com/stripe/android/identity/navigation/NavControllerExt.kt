package com.stripe.android.identity.navigation

import android.content.Context
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromRoute
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.viewmodel.IdentityViewModel

internal fun NavController.navigateToErrorScreenWithRequirementError(
    route: String,
    requirementError: VerificationPageDataRequirementError,
    identityViewModel: IdentityViewModel
) {
    identityViewModel.errorCause.postValue(
        IllegalStateException("VerificationPageDataRequirementError: $requirementError")
    )
    navigateTo(
        ErrorDestination(
            errorTitle = requirementError.title ?: context.getString(R.string.error),
            errorContent = requirementError.body
                ?: context.getString(R.string.unexpected_error_try_again),
            backButtonText = requirementError.backButtonText ?: context.getString(R.string.go_back),
            backButtonDestination =
            if (requirementError.requirement.matchesFromRoute(route)) {
                route
            } else {
                UNEXPECTED_ROUTE
            },
            shouldFail = false
        )
    )
}

internal fun NavController.navigateToErrorScreenWithDefaultValues(
    context: Context,
    cause: Throwable,
    identityViewModel: IdentityViewModel
) {
    identityViewModel.errorCause.postValue(cause)
    navigateTo(
        ErrorDestination(
            errorTitle = context.getString(R.string.error),
            errorContent = context.getString(R.string.unexpected_error_try_again),
            backButtonDestination = ConsentDestination.ROUTE.route,
            backButtonText = context.getString(R.string.go_back),
            shouldFail = false
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
    failedReason: Throwable,
    identityViewModel: IdentityViewModel
) {
    identityViewModel.errorCause.postValue(failedReason)
    navigateTo(
        ErrorDestination(
            errorTitle = context.getString(R.string.error),
            errorContent = context.getString(R.string.unexpected_error_try_again),
            backButtonText = context.getString(R.string.go_back),
            shouldFail = true
        )
    )
}
