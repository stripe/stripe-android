package com.stripe.android.identity.navigation

import android.content.Context
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromRoute
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Navigate to the final error screen with [requirementError], clicking the action button would
 * return to the previous screen.
 */
internal fun NavController.navigateToErrorScreenWithRequirementError(
    route: String,
    requirementError: VerificationPageDataRequirementError,
) {
    navigateTo(
        ErrorDestination(
            errorTitle = requirementError.title ?: context.getString(R.string.stripe_error),
            errorContent = requirementError.body
                ?: context.getString(R.string.stripe_unexpected_error_try_again),
            backButtonText = requirementError.backButtonText ?: context.getString(R.string.stripe_go_back),
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
 * Navigate to the final error screen with default values, clicking the action button would return
 * to the previous screen.
 */
internal fun NavController.navigateToErrorScreenWithDefaultValues(context: Context) {
    navigateTo(
        ErrorDestination(
            errorTitle = context.getString(R.string.stripe_error),
            errorContent = context.getString(R.string.stripe_unexpected_error_try_again),
            backButtonDestination = ConsentDestination.ROUTE.route,
            backButtonText = context.getString(R.string.stripe_go_back),
            shouldFail = false
        )
    )
}

/**
 * Navigate to the final error screen, clicking the action button would finish the verification
 * flow with failure.
 */
internal fun NavController.navigateToFinalErrorScreen(
    context: Context
) {
    navigateTo(context.finalErrorDestination())
}

/**
 * Route of all screens that collect front/back of a document.
 */
private val DOCUMENT_UPLOAD_ROUTES = setOf(
    IDUploadDestination.ROUTE.route,
    DriverLicenseUploadDestination.ROUTE.route,
    PassportUploadDestination.ROUTE.route,
    IDScanDestination.ROUTE.route,
    DriverLicenseScanDestination.ROUTE.route,
    PassportScanDestination.ROUTE.route
)

/**
 * Clear [IdentityViewModel.collectedData], [IdentityViewModel.documentFrontUploadedState] and
 * [IdentityViewModel.documentBackUploadedState] when the corresponding data collections screens
 * are about to be popped from navigation stack.
 * Then pop the screen by calling [NavController.navigateUp].
 */
internal fun NavController.clearDataAndNavigateUp(identityViewModel: IdentityViewModel): Boolean {
    currentBackStackEntry?.destination?.route?.let { currentEntryRoute ->
        if (DOCUMENT_UPLOAD_ROUTES.contains(currentEntryRoute)) {
            identityViewModel.clearUploadedData()
        }

        currentEntryRoute.routeToRequirement().forEach(identityViewModel::clearCollectedData)
    }

    previousBackStackEntry?.destination?.route?.let { previousEntryRoute ->
        if (DOCUMENT_UPLOAD_ROUTES.contains(previousEntryRoute)) {
            identityViewModel.clearUploadedData()
        }
        previousEntryRoute.routeToRequirement().forEach(identityViewModel::clearCollectedData)
    }

    return navigateUp()
}
