package com.stripe.android.identity.utils

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithDefaultValues
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithRequirementErrorAndDestination
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
    shouldNotSubmit: (verificationPageData: VerificationPageData) -> Boolean = { true },
    notSubmitBlock: ((verificationPageData: VerificationPageData) -> Unit)? = null
) {
    runCatching {
        identityViewModel.postVerificationPageData(collectedDataParam, clearDataParam)
    }.fold(
        onSuccess = { postedVerificationPageData ->
            if (postedVerificationPageData.hasError()) {
                navigateToRequirementErrorFragment(postedVerificationPageData.requirements.errors[0])
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
                                    navigateToRequirementErrorFragment(submittedVerificationPageData.requirements.errors[0])
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
                            navigateToDefaultErrorFragment()
                        }
                    )
                }
            }
        },
        onFailure = {
            navigateToDefaultErrorFragment()
        }
    )
}

/**
 * Navigate to [ErrorFragment] with [VerificationPageDataRequirementError].
 */
private fun Fragment.navigateToRequirementErrorFragment(
    requirementError: VerificationPageDataRequirementError
) {
    findNavController()
        .navigateToErrorFragmentWithRequirementErrorAndDestination(
            requirementError,
            // TODO(ccen) Determine the if the destination of the back button is always ConsentFragment
            R.id.action_errorFragment_to_consentFragment
        )
}

/**
 * Navigate to [ErrorFragment] with default values.
 */
internal fun Fragment.navigateToDefaultErrorFragment() {
    findNavController().navigateToErrorFragmentWithDefaultValues(requireContext())
}

private const val TAG = "NAVIGATION_UTIL"
