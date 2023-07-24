package com.stripe.android.identity

import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DebugDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.InitialLoadingDestination
import com.stripe.android.identity.navigation.clearDataAndNavigateUp
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Handles back button behavior based on current navigation status.
 */
internal class IdentityOnBackPressedHandler(
    private val context: Context,
    private val verificationFlowFinishable: VerificationFlowFinishable,
    private val navController: NavController,
    private val identityViewModel: IdentityViewModel
) : OnBackPressedCallback(true) {
    private var destination: NavDestination? = null
    private var args: Bundle? = null

    fun updateState(destination: NavDestination, args: Bundle?) {
        this.destination = destination
        this.args = args
    }

    override fun handleOnBackPressed() {
        // Don't navigate if there is a outstanding API request.
        if (identityViewModel.isSubmitting()) {
            return
        }
        if (navController.previousBackStackEntry?.destination?.route == InitialLoadingDestination.ROUTE.route ||
            navController.previousBackStackEntry?.destination?.route == DebugDestination.ROUTE.route
        ) {
            finishWithCancelResult(
                identityViewModel,
                verificationFlowFinishable,
                destination?.route?.routeToScreenName()
                    ?: IdentityAnalyticsRequestFactory.SCREEN_NAME_UNKNOWN
            )
        } else if (destination?.route == ConsentDestination.ROUTE.route) {
            showAboutToCancelAlert()
        } else {
            when (destination?.route) {
                ConfirmationDestination.ROUTE.route -> {
                    identityViewModel.sendSucceededAnalyticsRequestForNative()
                    verificationFlowFinishable.finishWithResult(
                        IdentityVerificationSheet.VerificationFlowResult.Completed
                    )
                }
                ErrorDestination.ROUTE.route -> {
                    if (args?.getBoolean(ErrorDestination.ARG_SHOULD_FAIL, false) == true) {
                        val failedReason = requireNotNull(
                            identityViewModel.errorCause.value
                        ) {
                            "Failed to get failedReason"
                        }

                        identityViewModel.sendAnalyticsRequest(
                            identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                                isFromFallbackUrl = false,
                                requireSelfie =
                                identityViewModel.verificationPage.value?.data?.requireSelfie(),
                                throwable = failedReason
                            )
                        )
                        verificationFlowFinishable.finishWithResult(
                            IdentityVerificationSheet.VerificationFlowResult.Failed(failedReason)
                        )
                    } else {
                        navController.clearDataAndNavigateUp(identityViewModel)
                    }
                }
                else -> {
                    navController.clearDataAndNavigateUp(identityViewModel)
                }
            }
        }
    }

    private fun showAboutToCancelAlert() {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.stripe_identity_confirm_cancel)
            .setPositiveButton(R.string.stripe_identity_yes) { _, _ ->
                finishWithCancelResult(
                    identityViewModel,
                    verificationFlowFinishable,
                    destination?.route?.routeToScreenName()
                        ?: IdentityAnalyticsRequestFactory.SCREEN_NAME_UNKNOWN
                )
            }.setNegativeButton(R.string.stripe_identity_no) { _, _ ->
            }
        builder.show()
    }

    private fun finishWithCancelResult(
        identityViewModel: IdentityViewModel,
        verificationFlowFinishable: VerificationFlowFinishable,
        lastScreeName: String
    ) {
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                isFromFallbackUrl = false,
                lastScreenName = lastScreeName,
                requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie()
            )
        )
        verificationFlowFinishable.finishWithResult(
            IdentityVerificationSheet.VerificationFlowResult.Canceled
        )
    }
}
