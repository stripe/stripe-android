package com.stripe.android.identity.navigation

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNSET_BUTTON_TEXT
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromRoute
import com.stripe.android.identity.networking.models.Requirement.Companion.supportsForceConfirm
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingBack
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingConsent
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingDocType
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

/**
 * Check the [VerificationPageData.requirements.missings] and navigate or invoke callbacks accordingly.
 */
internal suspend fun NavController.navigateOnVerificationPageData(
    verificationPageData: VerificationPageData,
    onMissingFront: () -> Unit,
    onMissingOtp: () -> Unit,
    onMissingBack: () -> Unit,
    onReadyToSubmit: suspend () -> Unit
) {
    if (verificationPageData.isMissingOtp()) {
        onMissingOtp()
    } else if (verificationPageData.isMissingConsent()) {
        navigateTo(ConsentDestination)
    }
    // TODO - remove this when backend removes docType
    else if (verificationPageData.isMissingDocType()) {
        navigateTo(DocSelectionDestination)
    } else if (verificationPageData.isMissingFront()) {
        onMissingFront()
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
