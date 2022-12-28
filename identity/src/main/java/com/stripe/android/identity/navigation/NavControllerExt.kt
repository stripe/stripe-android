package com.stripe.android.identity.navigation

import android.content.Context
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromRoute
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.utils.fragmentIdToRequirement
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * TODO(ccen) remove after all calls are migrated to IdentityViewModel
 */
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

internal fun NavController.navigateToErrorScreenWithRequirementError(
    route: String,
    requirementError: VerificationPageDataRequirementError,
) {
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

/**
 * TODO(ccen) remove after all calls are migrated to IdentityViewModel
 */
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

internal fun NavController.navigateToErrorScreenWithDefaultValues(context: Context) {
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
 *
 * TODO(ccen) remove after all calls are migrated to IdentityViewModel
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

/**
 * ID of all screens that collect front/back of a document.
 */
private val DOCUMENT_UPLOAD_SCREENS = setOf(
    R.id.IDUploadFragment,
    R.id.passportUploadFragment,
    R.id.driverLicenseUploadFragment,
    R.id.IDScanFragment,
    R.id.passportScanFragment,
    R.id.driverLicenseScanFragment
)

/**
 * Clear [IdentityViewModel.collectedData], [IdentityViewModel.documentFrontUploadedState] and
 * [IdentityViewModel.documentBackUploadedState] when the corresponding data collections screens
 * are about to be popped from navigation stack.
 * Then pop the screen by calling [NavController.navigateUp].
 */
internal fun NavController.clearDataAndNavigateUp(identityViewModel: IdentityViewModel): Boolean {
    currentBackStackEntry?.destination?.id?.let { currentEntryId ->
        if (DOCUMENT_UPLOAD_SCREENS.contains(currentEntryId)) {
            identityViewModel.clearUploadedData()
        }
        currentEntryId.fragmentIdToRequirement().forEach(identityViewModel::clearCollectedData)
    }

    previousBackStackEntry?.destination?.id?.let { previousEntryId ->
        if (DOCUMENT_UPLOAD_SCREENS.contains(previousEntryId)) {
            identityViewModel.clearUploadedData()
        }
        previousEntryId.fragmentIdToRequirement().forEach(identityViewModel::clearCollectedData)
    }

    return navigateUp()
}
