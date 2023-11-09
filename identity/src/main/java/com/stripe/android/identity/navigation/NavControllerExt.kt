package com.stripe.android.identity.navigation

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNSET_BUTTON_TEXT
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromRoute
import com.stripe.android.identity.networking.models.Requirement.Companion.supportsForceConfirm
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingBack
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingConsent
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingFront
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingIndividualRequirements
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingOtp
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingSelfie
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
    val requirement = requirementError.requirement

    // only show continue button when both text is not empty and requirement is supported
    val shouldShowContinueButton =
        !requirementError.continueButtonText.isNullOrEmpty() && requirement.supportsForceConfirm()

    // Received a button with continue text, but with unsupported requirement.
    //  Don't show continue button text and log an error
    if (!requirementError.continueButtonText.isNullOrEmpty() && !requirement.supportsForceConfirm()) {
        Log.e(NAV_CONTROLLER_TAG, "received unsupported requirement for forceConfirm: $requirement")
    }

    navigateTo(
        ErrorDestination(
            errorTitle = requirementError.title ?: context.getString(R.string.stripe_error),
            errorContent = requirementError.body
                ?: context.getString(R.string.stripe_unexpected_error_try_again),
            continueButtonText =
            if (shouldShowContinueButton && requirementError.continueButtonText != null) {
                requirementError.continueButtonText
            } else {
                UNSET_BUTTON_TEXT
            },
            continueButtonRequirement = if (shouldShowContinueButton) requirementError.requirement else null,
            backButtonText = requirementError.backButtonText
                ?: context.getString(R.string.stripe_go_back),
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
    DocumentScanDestination.ROUTE.route,
    DocumentUploadDestination.ROUTE.route
)

/**
 * Clear collected data before navigating up, each route maps to a set of [Requirement] and its corresponding field is
 * cleared through [IdentityViewModel.clearCollectedData].
 * Apart from collected data, the following document/selfie file upload related stats are also cleared.
 *  * When navigating up from [DocumentScanDestination] or [DocumentUploadDestination], clear document status.
 *  * When navigating up from [SelfieDestination], clear selfie status.
 *  * When navigating from any destination to [DocWarmupDestination], clearing both document and selfie status.
 */
internal fun NavController.clearDataAndNavigateUp(identityViewModel: IdentityViewModel): Boolean {
    // Clicking back from Document/Selfie screen, cleaning doc/selfie upload status
    currentBackStackEntry?.destination?.route?.let { currentEntryRoute ->
        if (DOCUMENT_UPLOAD_ROUTES.contains(currentEntryRoute)) {
            identityViewModel.clearDocumentUploadedState()
        }
        if (SelfieDestination.ROUTE.route == currentEntryRoute) {
            identityViewModel.clearSelfieUploadedState()
        }
        currentEntryRoute.routeToRequirement().forEach(identityViewModel::clearCollectedData)
    }

    previousBackStackEntry?.destination?.route?.let { previousEntryRoute ->
        // Clicking back from error screen and returning to Document/Selfie scan, cleaning doc/selfie upload status
        if (DOCUMENT_UPLOAD_ROUTES.contains(previousEntryRoute)) {
            identityViewModel.clearDocumentUploadedState()
        }
        if (SelfieDestination.ROUTE.route == previousEntryRoute) {
            identityViewModel.clearSelfieUploadedState()
        }

        // Back to [DocWarmupDestination], reset is needed because certain screens might be skipped from backstack.
        // E.g upload document -> land on [SelfieWarmupDestination] -> clicks back -> land on [DocWarmupDestination]
        //   Need to clear document and selfie status in this case, as document screen was not added to back stack and
        //   won't trigger currentBackStackEntry clean up logic.
        if (DocWarmupDestination.ROUTE.route == previousEntryRoute) {
            identityViewModel.resetAllUploadState()
            listOf(
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK,
                Requirement.FACE,
            ).forEach(identityViewModel::clearCollectedData)
        }
        previousEntryRoute.routeToRequirement().forEach(identityViewModel::clearCollectedData)
    }
    return navigateUp()
}

/**
 * Check the [VerificationPageData.requirements.missings] and navigate or invoke callbacks accordingly.
 */
internal suspend fun NavController.navigateOnVerificationPageData(
    verificationPageData: VerificationPageData,
    onMissingOtp: () -> Unit,
    onMissingBack: () -> Unit,
    onReadyToSubmit: suspend () -> Unit
) {
    if (verificationPageData.isMissingOtp()) {
        onMissingOtp()
    } else if (verificationPageData.isMissingConsent()) {
        navigateTo(ConsentDestination)
    } else if (verificationPageData.isMissingFront()) {
        navigateTo(DocWarmupDestination)
    } else if (verificationPageData.isMissingBack()) {
        onMissingBack()
    } else if (verificationPageData.isMissingSelfie()) {
        navigateTo(SelfieWarmupDestination)
    } else if (verificationPageData.isMissingIndividualRequirements()) {
        navigateTo(IndividualDestination)
    } else {
        onReadyToSubmit()
    }
}

const val NAV_CONTROLLER_TAG = "NavController"
