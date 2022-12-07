package com.stripe.android.identity.utils

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_DRIVER_LICENSE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_PASSPORT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_PASSPORT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.navigation.IdentityTopLevelDestination
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.navigation.navigateToErrorScreenWithFailedReason
import com.stripe.android.identity.navigation.navigateToErrorScreenWithRequirementError
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.hasError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * A util method for a [Fragment] to post [CollectedDataParam] and resolve its [VerificationPageData].
 *
 * * If the initial post fails, navigate to [ErrorFragment] with default values.
 * * If the initial post succeeds, check if the response [VerificationPageData] has error.
 *    * If it has error, then navigate to [ErrorFragment] with the error data.
 *    * Otherwise, check [notSubmitBlock].
 *      * If it's not null invoke it
 *      * Otherwise, post submit
 *        * If submit succeeds, navigate to [ConfirmationFragment]
 *        * If submit fails, navigate to [ErrorFragment]
 *
 *
 * @param identityViewModel: [IdentityViewModel] to fire requests.
 * @param collectedDataParam: parameter collected from UI response, posted to [IdentityViewModel.postVerificationPageData].
 * @param notSubmitBlock: If not null, execute this block instead of submit by
 * [IdentityViewModel.postVerificationPageSubmit] after [IdentityViewModel.postVerificationPageData] returns.
 */
internal suspend fun Fragment.postVerificationPageDataAndMaybeSubmit(
    identityViewModel: IdentityViewModel,
    collectedDataParam: CollectedDataParam,
    fromRoute: String,
    notSubmitBlock: ((verificationPageData: VerificationPageData) -> Unit)? = null
) {
    postVerificationPageData(
        identityViewModel,
        collectedDataParam,
        fromRoute
    ) { postedVerificationPageData ->
        notSubmitBlock?.invoke(postedVerificationPageData) ?: run {
            submitVerificationPageDataAndNavigate(identityViewModel, fromRoute)
        }
    }
}

/**
 * Check if selfie is required from [VerificationPage], navigate to Ω fragment if so, otherwise
 * submit the verification.
 */
internal suspend fun Fragment.navigateToSelfieOrSubmit(
    verificationPage: VerificationPage,
    identityViewModel: IdentityViewModel,
    fromRoute: String
) {
    if (verificationPage.requireSelfie()) {
        navigateOnResume(SelfieDestination)
    } else {
        submitVerificationPageDataAndNavigate(
            identityViewModel,
            fromRoute
        )
    }
}

/**
 * Submit the verification, if submit has error, navigates to error fragment with the error,
 * if [VerificationPageData.submitted] is true, navigates to confirm fragment,
 * otherwise navigate to generic error fragment.
 */
internal suspend fun Fragment.submitVerificationPageDataAndNavigate(
    identityViewModel: IdentityViewModel,
    fromRoute: String
) {
    runCatching {
        identityViewModel.postVerificationPageSubmit()
    }.fold(
        onSuccess = { submittedVerificationPageData ->
            when {
                submittedVerificationPageData.hasError() -> {
                    navigateToRequirementErrorFragment(
                        fromRoute,
                        submittedVerificationPageData.requirements.errors[0],
                        identityViewModel
                    )
                }
                submittedVerificationPageData.submitted -> {
                    navigateOnResume(ConfirmationDestination)
                }
                else -> {
                    "VerificationPage submit failed".let { msg ->
                        Log.e(TAG, msg)
                        navigateToDefaultErrorFragment(msg, identityViewModel)
                    }
                }
            }
        },
        onFailure = {
            Log.e(TAG, "Failed to postVerificationPageSubmit: $it")
            navigateToDefaultErrorFragment(it, identityViewModel)
        }
    )
}

/**
 * Post Verification data and callback in [onCorrectResponse] if the response is without error.
 * Otherwise navigate to error fragment.
 */
internal suspend fun Fragment.postVerificationPageData(
    identityViewModel: IdentityViewModel,
    collectedDataParam: CollectedDataParam,
    fromRoute: String,
    onCorrectResponse: suspend ((verificationPageDataWithNoError: VerificationPageData) -> Unit) = {}
) {
    identityViewModel.screenTracker.screenTransitionStart(
        fromRoute.routeToScreenName()
    )
    runCatching {
        identityViewModel.postVerificationPageData(collectedDataParam)
    }.fold(
        onSuccess = { postedVerificationPageData ->
            if (postedVerificationPageData.hasError()) {
                navigateToRequirementErrorFragment(
                    fromRoute,
                    postedVerificationPageData.requirements.errors[0],
                    identityViewModel
                )
            } else {
                onCorrectResponse(postedVerificationPageData)
            }
        },
        onFailure = {
            Log.e(TAG, "Failed to postVerificationPageData: $it")
            navigateToDefaultErrorFragment(it, identityViewModel)
        }
    )
}

/**
 * Navigate to [ErrorFragment] with [VerificationPageDataRequirementError].
 */
private fun Fragment.navigateToRequirementErrorFragment(
    fromRoute: String,
    requirementError: VerificationPageDataRequirementError,
    identityViewModel: IdentityViewModel
) {
    repeatOnResume {
        findNavController()
            .navigateToErrorScreenWithRequirementError(
                fromRoute,
                requirementError,
                identityViewModel
            )
    }
}

/**
 * Navigate to [ErrorFragment] with default values and a cause.
 */
internal fun Fragment.navigateToDefaultErrorFragment(
    cause: Throwable,
    identityViewModel: IdentityViewModel
) {
    repeatOnResume {
        findNavController().navigateToErrorScreenWithDefaultValues(
            requireContext(),
            cause,
            identityViewModel
        )
    }
}

/**
 * Navigate to [ErrorFragment] with default values and a message.
 */
internal fun Fragment.navigateToDefaultErrorFragment(
    message: String,
    identityViewModel: IdentityViewModel
) {
    repeatOnResume {
        findNavController().navigateToErrorScreenWithDefaultValues(
            requireContext(),
            IllegalStateException(message),
            identityViewModel
        )
    }
}

/**
 * Navigate to [ErrorFragment] as final destination.
 */
internal fun Fragment.navigateToErrorFragmentWithFailedReason(
    failedReason: Throwable,
    identityViewModel: IdentityViewModel
) {
    repeatOnResume {
        findNavController().navigateToErrorScreenWithFailedReason(
            requireContext(),
            failedReason,
            identityViewModel
        )
    }
}

/**
 * Try navigate to a destination when the fragment is in resume state.
 * If app is backgrounded, navigate when it's brought to foreground.
 */
internal fun Fragment.navigateOnResume(destination: IdentityTopLevelDestination) {
    repeatOnResume {
        findNavController().navigateTo(destination)
    }
}

private fun Fragment.repeatOnResume(block: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            block()
        }
    }
}

internal fun Int.fragmentIdToRequirement(): List<Requirement> = when (this) {
    R.id.consentFragment -> {
        listOf(Requirement.BIOMETRICCONSENT)
    }
    R.id.docSelectionFragment -> {
        listOf(Requirement.IDDOCUMENTTYPE)
    }
    R.id.IDUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.passportUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.driverLicenseUploadFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.IDScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.passportScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.driverLicenseScanFragment -> {
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    }
    R.id.selfieFragment -> {
        listOf(Requirement.FACE)
    }
    else -> {
        listOf()
    }
}

internal fun Int.fragmentIdToScreenName(): String = when (this) {
    R.id.consentFragment -> {
        SCREEN_NAME_CONSENT
    }
    R.id.docSelectionFragment -> {
        SCREEN_NAME_DOC_SELECT
    }
    R.id.IDScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_ID
    }
    R.id.passportScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_PASSPORT
    }
    R.id.driverLicenseScanFragment -> {
        SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
    }
    R.id.IDUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_ID
    }
    R.id.passportUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_PASSPORT
    }
    R.id.driverLicenseUploadFragment -> {
        SCREEN_NAME_FILE_UPLOAD_DRIVER_LICENSE
    }
    R.id.selfieFragment -> {
        SCREEN_NAME_SELFIE
    }
    R.id.confirmationFragment -> {
        SCREEN_NAME_CONFIRMATION
    }
    R.id.cameraPermissionDeniedFragment -> {
        SCREEN_NAME_ERROR
    }
    R.id.errorFragment -> {
        SCREEN_NAME_ERROR
    }
    R.id.couldNotCaptureFragment -> {
        SCREEN_NAME_ERROR
    }
    else -> {
        throw IllegalArgumentException("Invalid fragment ID: $this")
    }
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

private const val TAG = "NAVIGATION_UTIL"
