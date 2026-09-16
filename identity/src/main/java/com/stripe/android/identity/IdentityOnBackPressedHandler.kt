package com.stripe.android.identity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DebugDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.InitialLoadingDestination
import com.stripe.android.identity.navigation.NetworkedIdentityDestination
import com.stripe.android.identity.navigation.clearDataAndNavigateUp
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Handles back button behavior based on current navigation status.
 */
internal class IdentityOnBackPressedHandler(
    private val verificationFlowFinishable: VerificationFlowFinishable,
    private val navController: NavController,
    private val identityViewModel: IdentityViewModel,
) : OnBackPressedCallback(true) {
    private var destination: NavDestination? = null
    private var args: Bundle? = null

    fun updateState(destination: NavDestination, args: Bundle?) {
        this.destination = destination
        this.args = args
    }

    override fun handleOnBackPressed() {
        if (destination?.route == NetworkedIdentityDestination.ROUTE.route) {
            identityViewModel.networkedIdentityViewModel?.cancel()
            return
        }
        // Don't navigate if there is a outstanding API request.
        if (identityViewModel.isSubmitting()) {
            return
        }
        if (shouldCloseWithCancel()) {
            finishWithCancelResult(
                destination?.route?.routeToScreenName()
                    ?: IdentityAnalyticsRequestFactory.SCREEN_NAME_UNKNOWN
            )
        } else {
            when (destination?.route) {
                ConfirmationDestination.ROUTE.route -> {
                    identityViewModel.sendSucceededAnalyticsRequestForNative()
                    verificationFlowFinishable.finishWithResult(
                        IdentityVerificationSheet.VerificationFlowResult.Completed
                    )
                }
                ErrorDestination.ROUTE.route -> handleErrorScreenBack()
                else -> {
                    navController.clearDataAndNavigateUp(identityViewModel)
                }
            }
        }
    }

    private fun shouldCloseWithCancel(): Boolean {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        val networkedRoot = identityViewModel.hasEnteredNetworkedIdentity && previousRoute == null &&
            destination?.route !in setOf(ConfirmationDestination.ROUTE.route, ErrorDestination.ROUTE.route)
        return previousRoute == InitialLoadingDestination.ROUTE.route ||
            previousRoute == DebugDestination.ROUTE.route ||
            destination?.route == ConsentDestination.ROUTE.route || networkedRoot
    }

    private fun handleErrorScreenBack() {
        if (args?.getBoolean(ErrorDestination.ARG_SHOULD_FAIL, false) == true) {
            val failedReason = requireNotNull(identityViewModel.errorCause.value) { "Failed to get failedReason" }
            identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                isFromFallbackUrl = false,
                requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie(),
                throwable = failedReason,
                lastScreenName = identityViewModel.analyticsLastScreenName
            )
            verificationFlowFinishable.finishWithResult(
                IdentityVerificationSheet.VerificationFlowResult.Failed(failedReason)
            )
        } else if (identityViewModel.hasEnteredNetworkedIdentity && navController.previousBackStackEntry == null) {
            finishWithCancelResult(IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR)
        } else {
            navController.clearDataAndNavigateUp(identityViewModel)
        }
    }

    private fun finishWithCancelResult(lastScreenName: String) {
        identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
            isFromFallbackUrl = false,
            lastScreenName = identityViewModel.analyticsLastScreenName ?: lastScreenName,
            requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie()
        )
        verificationFlowFinishable.finishWithResult(
            IdentityVerificationSheet.VerificationFlowResult.Canceled
        )
    }
}
