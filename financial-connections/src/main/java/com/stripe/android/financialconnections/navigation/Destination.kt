package com.stripe.android.financialconnections.navigation

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
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod
import com.stripe.android.financialconnections.presentation.parentViewModel

internal sealed class Destination(
    protected val route: String,
    protected val paramKeys: List<String>,
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
                    referrer = referrer(navBackStackEntry),
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
        composable: @Composable (NavBackStackEntry) -> Unit
    ) : Destination(
        route = route,
        paramKeys = listOf(KEY_REFERRER),
        screenBuilder = composable
    ) {
        override operator fun invoke(
            referrer: Pane?,
            args: Map<String, String?>
        ): String = route.appendParamValues(
            KEY_REFERRER to referrer?.value
        )
    }

    object InstitutionPicker : NoArgumentsDestination(
        Pane.INSTITUTION_PICKER.value,
        composable = { InstitutionPickerScreen() }
    )

    object Consent : NoArgumentsDestination(
        route = Pane.CONSENT.value,
        composable = { ConsentScreen() }
    )

    object PartnerAuth : NoArgumentsDestination(
        route = Pane.PARTNER_AUTH.value,
        composable = { PartnerAuthScreen() }
    )

    object AccountPicker : NoArgumentsDestination(
        route = Pane.ACCOUNT_PICKER.value,
        composable = { AccountPickerScreen() }
    )

    object Success : NoArgumentsDestination(
        route = Pane.SUCCESS.value,
        composable = { SuccessScreen() }
    )

    object ManualEntry : NoArgumentsDestination(
        route = Pane.MANUAL_ENTRY.value,
        composable = { ManualEntryScreen() }
    )

    object AttachLinkedPaymentAccount : NoArgumentsDestination(
        route = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT.value,
        composable = { AttachPaymentScreen() }
    )

    object NetworkingLinkSignup : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_SIGNUP_PANE.value,
        composable = { NetworkingLinkSignupScreen() }
    )

    object NetworkingLinkLoginWarmup : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_LOGIN_WARMUP.value,
        composable = { NetworkingLinkLoginWarmupScreen() }
    )

    object NetworkingLinkVerification : NoArgumentsDestination(
        route = Pane.NETWORKING_LINK_VERIFICATION.value,
        composable = { NetworkingLinkVerificationScreen() }
    )

    object NetworkingSaveToLinkVerification : NoArgumentsDestination(
        route = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION.value,
        composable = { NetworkingSaveToLinkVerificationScreen() }
    )

    object LinkAccountPicker : NoArgumentsDestination(
        route = Pane.LINK_ACCOUNT_PICKER.value,
        composable = {
            LinkAccountPickerScreen()
        },
    )

    object LinkStepUpVerification : NoArgumentsDestination(
        route = Pane.LINK_STEP_UP_VERIFICATION.value,
        composable = { LinkStepUpVerificationScreen() }
    )

    object Reset : NoArgumentsDestination(
        route = Pane.RESET.value,
        composable = { ResetScreen() }
    )

    object BankAuthRepair : NoArgumentsDestination(
        route = Pane.BANK_AUTH_REPAIR.value,
        composable = { BankAuthRepairScreen() }
    )

    object ManualEntrySuccess : Destination(
        route = Pane.MANUAL_ENTRY_SUCCESS.value,
        paramKeys = listOf(KEY_REFERRER, KEY_MICRODEPOSITS, KEY_LAST4),
        screenBuilder = { ManualEntrySuccessScreen(it) }
    ) {

        fun argMap(
            microdepositVerificationMethod: MicrodepositVerificationMethod,
            last4: String?
        ): Map<String, String?> = mapOf(
            KEY_MICRODEPOSITS to microdepositVerificationMethod.value,
            KEY_LAST4 to last4
        )

        fun microdeposits(backStackEntry: NavBackStackEntry): MicrodepositVerificationMethod =
            backStackEntry.arguments
                ?.getString(KEY_MICRODEPOSITS)
                ?.let { value ->
                    MicrodepositVerificationMethod.values().firstOrNull { it.value == value }
                } ?: MicrodepositVerificationMethod.UNKNOWN

        fun last4(backStackEntry: NavBackStackEntry): String? =
            backStackEntry.arguments?.getString(KEY_LAST4)

        override fun invoke(
            referrer: Pane?,
            args: Map<String, String?>
        ): String = route.appendParamValues(
            KEY_REFERRER to referrer?.value,
            KEY_MICRODEPOSITS to args[KEY_MICRODEPOSITS],
            KEY_LAST4 to args[KEY_LAST4]
        )
    }

    companion object {
        private fun referrer(entry: NavBackStackEntry): Pane? = entry.arguments
            ?.getString(KEY_REFERRER)
            ?.let { value -> Pane.values().firstOrNull { it.value == value } }

        const val KEY_REFERRER = "referrer"
        const val KEY_MICRODEPOSITS = "microdeposits"
        const val KEY_LAST4 = "last4"
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
