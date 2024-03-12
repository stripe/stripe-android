package com.stripe.android.financialconnections.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerScreen
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentScreen
import com.stripe.android.financialconnections.features.bankauthrepair.BankAuthRepairScreen
import com.stripe.android.financialconnections.features.consent.ConsentScreen
import com.stripe.android.financialconnections.features.error.ErrorScreen
import com.stripe.android.financialconnections.features.exit.ExitModal
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerScreen
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationScreen
import com.stripe.android.financialconnections.features.manualentry.ManualEntryScreen
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessScreen
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupScreen
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupScreen
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationScreen
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationScreen
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.bottomsheet.bottomSheet
import com.stripe.android.financialconnections.presentation.parentViewModel

internal sealed class Destination(
    val route: String,
    protected val paramKeys: List<String>,
    val launchAsRoot: Boolean,
    protected val screenBuilder: @Composable (NavBackStackEntry) -> Unit
) {
    val fullRoute: String = if (paramKeys.isEmpty()) {
        route
    } else {
        val builder = StringBuilder(route)
        paramKeys.forEach { builder.append("/{$it}") }
        builder.toString()
    }

    @Composable
    fun Composable(navBackStackEntry: NavBackStackEntry) {
        val viewModel = parentViewModel()
        // prevents the launched event to be re-triggered on configuration changes
        var paneLaunchedTriggered by rememberSaveable { mutableStateOf(false) }
        if (!paneLaunchedTriggered) {
            LaunchedEffect(Unit) {
                viewModel.onPaneLaunched(
                    referrer = referrer(navBackStackEntry.arguments),
                    pane = navBackStackEntry.destination.pane
                )
                paneLaunchedTriggered = true
            }
        }
        screenBuilder(navBackStackEntry)
    }

    /**
     * Builds the navigation route with arg keys and values.
     *
     * @param args a map of arguments to be appended to the route
     */
    abstract operator fun invoke(
        referrer: Pane?,
        args: Map<String, String?> = emptyMap()
    ): String

    sealed class NoArgumentsDestination(
        route: String,
        launchAsRoot: Boolean,
        composable: @Composable (NavBackStackEntry) -> Unit
    ) : Destination(
        route = route,
        paramKeys = listOf(KEY_REFERRER),
        launchAsRoot = launchAsRoot,
        screenBuilder = composable
    ) {
        override operator fun invoke(
            referrer: Pane?,
            args: Map<String, String?>
        ): String = route.appendParamValues(
            KEY_REFERRER to referrer?.value
        )
    }

    data object InstitutionPicker : NoArgumentsDestination(
        route = Pane.INSTITUTION_PICKER.value,
        launchAsRoot = false,
        composable = { InstitutionPickerScreen() }
    )

    data object Consent : NoArgumentsDestination(
        route = Pane.CONSENT.value,
        launchAsRoot = false,
        composable = { ConsentScreen() }
    )

    data object PartnerAuthDrawer : NoArgumentsDestination(
        route = Pane.PARTNER_AUTH_DRAWER.value,
        launchAsRoot = false,
        composable = { PartnerAuthScreen(inModal = true) }
    )

    data object PartnerAuth : NoArgumentsDestination(
        route = Pane.PARTNER_AUTH.value,
        launchAsRoot = false,
        composable = { PartnerAuthScreen(inModal = false) }
    )

    data object AccountPicker : NoArgumentsDestination(
        route = Pane.ACCOUNT_PICKER.value,
        launchAsRoot = true,
        composable = { AccountPickerScreen() }
    )

    data object Success : NoArgumentsDestination(
        route = Pane.SUCCESS.value,
        launchAsRoot = true,
        composable = { SuccessScreen() }
    )

    data object ManualEntry : NoArgumentsDestination(
        route = Pane.MANUAL_ENTRY.value,
        launchAsRoot = false,
        composable = { ManualEntryScreen() }
    )

    data object AttachLinkedPaymentAccount : NoArgumentsDestination(
        route = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT.value,
        launchAsRoot = true,
        composable = { AttachPaymentScreen() }
    )

    data object NetworkingLinkSignup : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_SIGNUP_PANE.value,
        launchAsRoot = true,
        composable = { NetworkingLinkSignupScreen() }
    )

    data object NetworkingLinkLoginWarmup : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_LOGIN_WARMUP.value,
        launchAsRoot = false,
        composable = { NetworkingLinkLoginWarmupScreen(it) }
    )

    data object NetworkingLinkVerification : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_VERIFICATION.value,
        launchAsRoot = false,
        composable = { NetworkingLinkVerificationScreen() }
    )

    data object NetworkingSaveToLinkVerification : NoArgumentsDestination(
        route = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION.value,
        launchAsRoot = false,
        composable = { NetworkingSaveToLinkVerificationScreen() }
    )

    data object LinkAccountPicker : NoArgumentsDestination(
        route = Pane.LINK_ACCOUNT_PICKER.value,
        launchAsRoot = true,
        composable = { LinkAccountPickerScreen() },
    )

    data object LinkStepUpVerification : NoArgumentsDestination(
        route = Pane.LINK_STEP_UP_VERIFICATION.value,
        launchAsRoot = true,
        composable = { LinkStepUpVerificationScreen() }
    )

    data object Reset : NoArgumentsDestination(
        route = Pane.RESET.value,
        launchAsRoot = false,
        composable = { ResetScreen() }
    )

    data object Exit : NoArgumentsDestination(
        route = Pane.EXIT.value,
        launchAsRoot = false,
        composable = { ExitModal(it) }
    )

    data object Error : NoArgumentsDestination(
        route = Pane.UNEXPECTED_ERROR.value,
        launchAsRoot = true,
        composable = { ErrorScreen() }
    )

    data object BankAuthRepair : NoArgumentsDestination(
        route = Pane.BANK_AUTH_REPAIR.value,
        launchAsRoot = false,
        composable = { BankAuthRepairScreen() }
    )

    data object ManualEntrySuccess : NoArgumentsDestination(
        route = Pane.MANUAL_ENTRY_SUCCESS.value,
        launchAsRoot = true,
        composable = { ManualEntrySuccessScreen() }
    )

    companion object {

        internal fun referrer(args: Bundle?): Pane? = args
            ?.getString(KEY_REFERRER)
            ?.let { value -> Pane.entries.firstOrNull { it.value == value } }

        internal fun fromRoute(route: String): Destination? {
            return paneToDestination.keys.firstOrNull { it.destination.route == route }?.destination
        }

        const val KEY_REFERRER = "referrer"
    }
}

internal fun String.appendParamValues(vararg params: Pair<String, Any?>): String {
    val builder = StringBuilder(this)

    params.forEach {
        it.second?.toString()?.let { arg ->
            builder.append("/$arg")
        }
    }

    return builder.toString()
}

internal fun NavGraphBuilder.composable(
    destination: Destination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
) {
    composable(
        route = destination.fullRoute,
        arguments = arguments,
        deepLinks = deepLinks,
        content = { destination.Composable(navBackStackEntry = it) }
    )
}

internal fun NavGraphBuilder.bottomSheet(
    destination: Destination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
) {
    bottomSheet(
        route = destination.fullRoute,
        arguments = arguments,
        deepLinks = deepLinks,
        content = { destination.Composable(navBackStackEntry = it) }
    )
}
