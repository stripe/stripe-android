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
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredModal
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
import com.stripe.android.financialconnections.features.notice.NoticeSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.bottomsheet.bottomSheet
import com.stripe.android.financialconnections.presentation.parentViewModel

/**
 * Represents a destination in the financial connections flow.
 *
 * @param route The route of the destination.
 * @param closeWithoutConfirmation Whether the destination should be close without showing a confirmation modal.
 * @param logPaneLaunched Whether the destination is a real pane that should log the pane launched event.
 */
internal sealed class Destination(
    protected val route: String,
    val closeWithoutConfirmation: Boolean,
    val logPaneLaunched: Boolean,
    paramKeys: List<String> = listOf(KEY_REFERRER),
    protected val composable: @Composable (NavBackStackEntry) -> Unit
) {
    val fullRoute: String by lazy {
        val placeholders = paramKeys.map { "{$it}" }
        route.appendParamValues(placeholders)
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
        composable(navBackStackEntry)
    }

    /**
     * Builds the navigation route with provided [referrer].
     */
    operator fun invoke(
        referrer: Pane,
    ): String = route.appendParamValues(listOf(referrer.value))

    data object InstitutionPicker : Destination(
        route = Pane.INSTITUTION_PICKER.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { InstitutionPickerScreen() }
    )

    data object Consent : Destination(
        route = Pane.CONSENT.value,
        closeWithoutConfirmation = true,
        logPaneLaunched = true,
        composable = { ConsentScreen() }
    )

    data object PartnerAuthDrawer : Destination(
        route = Pane.PARTNER_AUTH_DRAWER.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { PartnerAuthScreen(inModal = true) }
    )

    data object PartnerAuth : Destination(
        route = Pane.PARTNER_AUTH.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { PartnerAuthScreen(inModal = false) }
    )

    data object AccountPicker : Destination(
        route = Pane.ACCOUNT_PICKER.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { AccountPickerScreen() }
    )

    data object Success : Destination(
        route = Pane.SUCCESS.value,
        closeWithoutConfirmation = true,
        logPaneLaunched = true,
        composable = { SuccessScreen() }
    )

    data object ManualEntry : Destination(
        route = Pane.MANUAL_ENTRY.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { ManualEntryScreen() }
    )

    data object AttachLinkedPaymentAccount : Destination(
        route = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { AttachPaymentScreen() }
    )

    data object NetworkingLinkSignup : Destination(
        route = Pane.NETWORKING_LINK_SIGNUP_PANE.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { NetworkingLinkSignupScreen() }
    )

    data object NetworkingLinkLoginWarmup : Destination(
        route = Pane.NETWORKING_LINK_LOGIN_WARMUP.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { NetworkingLinkLoginWarmupScreen(it) }
    )

    data object NetworkingLinkVerification : Destination(
        route = Pane.NETWORKING_LINK_VERIFICATION.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { NetworkingLinkVerificationScreen() }
    )

    data object NetworkingSaveToLinkVerification : Destination(
        route = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { NetworkingSaveToLinkVerificationScreen() }
    )

    data object LinkAccountPicker : Destination(
        route = Pane.LINK_ACCOUNT_PICKER.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { LinkAccountPickerScreen() },
    )

    data object LinkStepUpVerification : Destination(
        route = Pane.LINK_STEP_UP_VERIFICATION.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { LinkStepUpVerificationScreen() }
    )

    data object Reset : Destination(
        route = Pane.RESET.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { ResetScreen() }
    )

    data object Exit : Destination(
        route = Pane.EXIT.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = false,
        composable = { ExitModal(it) }
    )

    data object Notice : Destination(
        route = Pane.NOTICE.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = false,
        composable = { NoticeSheet(it) },
    )

    data object AccountUpdateRequired : Destination(
        route = Pane.ACCOUNT_UPDATE_REQUIRED.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = false,
        composable = { AccountUpdateRequiredModal(it) },
    )

    data object Error : Destination(
        route = Pane.UNEXPECTED_ERROR.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = false,
        composable = { ErrorScreen() }
    )

    data object BankAuthRepair : Destination(
        route = Pane.BANK_AUTH_REPAIR.value,
        closeWithoutConfirmation = false,
        logPaneLaunched = true,
        composable = { BankAuthRepairScreen() }
    )

    data object ManualEntrySuccess : Destination(
        route = Pane.MANUAL_ENTRY_SUCCESS.value,
        closeWithoutConfirmation = true,
        logPaneLaunched = true,
        composable = { ManualEntrySuccessScreen() }
    )

    companion object {
        internal fun referrer(args: Bundle?): Pane? = args
            ?.getString(KEY_REFERRER)
            ?.let { value -> Pane.entries.firstOrNull { it.value == value } }

        const val KEY_REFERRER = "referrer"
    }
}

internal fun String.appendParamValues(params: List<String>): String {
    return buildString {
        append(this@appendParamValues)
        params.forEach { append("/$it") }
    }
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
