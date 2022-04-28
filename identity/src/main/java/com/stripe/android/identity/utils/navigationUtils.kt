package com.stripe.android.identity.utils

import android.util.Log
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithDefaultValues
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithFailedReason
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithRequirementError
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.hasError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * A util method for a [Fragment] to post [CollectedDataParam] and resolve its [VerificationPageData].
 *
 * * If the initial post fails, navigate to [ErrorFragment] with default values.
 * * If the initial post succeeds, check if the response [VerificationPageData] has error.
 *    * If it has error, then navigate to [ErrorFragment] with the error data.
 *    * Otherwise, check [shouldNotSubmit].
 *      * If it's true invoke [notSubmitBlock]
 *      * If it's false, post submit
 *        * If submit succeeds, navigate to [ConfirmationFragment]
 *        * If submit fails, navigate to [ErrorFragment]
 *
 *
 * @param identityViewModel: [IdentityViewModel] to fire requests.
 * @param collectedDataParam: parameter collected from UI response, posted to [IdentityViewModel.postVerificationPageData].
 * @param shouldNotSubmit: A condition check block to decide when [VerificationPageData]
 * is returned without error, whether to continue submit by [IdentityViewModel.postVerificationPageSubmit].
 * @param notSubmitBlock: A block to execute when [shouldNotSubmit] returns true.
 */
internal suspend fun Fragment.postVerificationPageDataAndMaybeSubmit(
    identityViewModel: IdentityViewModel,
    collectedDataParam: CollectedDataParam,
    clearDataParam: ClearDataParam,
    @IdRes fromFragment: Int,
    shouldNotSubmit: (verificationPageData: VerificationPageData) -> Boolean = { true },
    notSubmitBlock: ((verificationPageData: VerificationPageData) -> Unit)? = null
) {
    runCatching {
        identityViewModel.postVerificationPageData(collectedDataParam, clearDataParam)
    }.fold(
        onSuccess = { postedVerificationPageData ->
            if (postedVerificationPageData.hasError()) {
                navigateToRequirementErrorFragment(
                    fromFragment,
                    postedVerificationPageData.requirements.errors[0]
                )
            } else {
                if (shouldNotSubmit(postedVerificationPageData)) {
                    notSubmitBlock?.invoke(postedVerificationPageData)
                } else {
                    runCatching {
                        identityViewModel.postVerificationPageSubmit()
                    }.fold(
                        onSuccess = { submittedVerificationPageData ->
                            when {
                                submittedVerificationPageData.hasError() -> {
                                    navigateToRequirementErrorFragment(
                                        fromFragment,
                                        submittedVerificationPageData.requirements.errors[0]
                                    )
                                }
                                submittedVerificationPageData.submitted -> {
                                    findNavController()
                                        .navigate(R.id.action_global_confirmationFragment)
                                }
                                else -> {
                                    Log.e(TAG, "VerificationPage submit failed")
                                    navigateToDefaultErrorFragment()
                                }
                            }
                        },
                        onFailure = {
                            Log.e(TAG, "Failed to postVerificationPageSubmit: $it")
                            navigateToDefaultErrorFragment()
                        }
                    )
                }
            }
        },
        onFailure = {
            Log.e(TAG, "Failed to postVerificationPageData: $it")
            navigateToDefaultErrorFragment()
        }
    )
}

/**
 * Navigate to [ErrorFragment] with [VerificationPageDataRequirementError].
 */
private fun Fragment.navigateToRequirementErrorFragment(
    @IdRes fromFragment: Int,
    requirementError: VerificationPageDataRequirementError,
) {
    findNavController()
        .navigateToErrorFragmentWithRequirementError(
            fromFragment,
            requirementError,
        )
}

/**
 * Navigate to [ErrorFragment] with default values.
 */
internal fun Fragment.navigateToDefaultErrorFragment() {
    findNavController().navigateToErrorFragmentWithDefaultValues(requireContext())
}

/**
 * Navigate to [ErrorFragment] as final destination.
 */
internal fun Fragment.navigateToErrorFragmentWithFailedReason(failedReason: Throwable) {
    findNavController().navigateToErrorFragmentWithFailedReason(requireContext(), failedReason)
}

/**
 * Navigate to upload fragment with shouldShowTakePhoto argument.
 */
internal fun Fragment.navigateToUploadFragment(
    @IdRes destinationId: Int,
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean
) {
    findNavController().navigate(
        destinationId,
        bundleOf(
            ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
            ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
        )
    )
}

/**
 * Navigates up with this NavController, if the previousBackStackEntry is upload fragment,
 * sets [ARG_IS_NAVIGATED_UP_TO] to true as its NavArgument.
 *
 * This makes it possible to tell in upload fragment whether it is reached through
 * [NavController.navigateUp] or [NavController.navigate].
 */
internal fun NavController.navigateUpAndSetArgForUploadFragment(): Boolean {
    if (isBackingToUploadFragment()) {
        previousBackStackEntry?.destination?.addArgument(
            ARG_IS_NAVIGATED_UP_TO,
            NavArgument.Builder()
                .setDefaultValue(true)
                .build()
        )
    }
    return navigateUp()
}

internal fun NavController.isNavigatedUpTo(): Boolean {
    return this.currentDestination?.arguments?.get(ARG_IS_NAVIGATED_UP_TO)?.defaultValue
        as? Boolean == true
}

private fun NavController.isBackingToUploadFragment() =
    previousBackStackEntry?.destination?.id == R.id.IDUploadFragment ||
        previousBackStackEntry?.destination?.id == R.id.passportUploadFragment ||
        previousBackStackEntry?.destination?.id == R.id.driverLicenseUploadFragment

/**
 * Argument to indicate if take photo option should be shown when picking an image.
 */
internal const val ARG_SHOULD_SHOW_TAKE_PHOTO = "shouldShowTakePhoto"

/**
 * Argument to indicate if choose photo option should be shown when picking an image.
 */
internal const val ARG_SHOULD_SHOW_CHOOSE_PHOTO = "shouldShowChoosePhoto"

/**
 * Navigation Argument to indicate if the current Fragment is reached through navigateUp.
 */
internal const val ARG_IS_NAVIGATED_UP_TO = "isNavigatedUpTo"

private const val TAG = "NAVIGATION_UTIL"
