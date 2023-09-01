package com.stripe.android.financialconnections.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod

internal sealed class Destination(protected val route: String, vararg params: String) {
    val fullRoute: String = if (params.isEmpty()) route else {
        val builder = StringBuilder(route)
        params.forEach { builder.append("/{${it}}") }
        builder.toString()
    }

    abstract operator fun invoke(args: Map<String, String?> = emptyMap()): String

    sealed class NoArgumentsDestination(route: String) : Destination(route) {
        override operator fun invoke(args: Map<String, String?>): String = route
    }

    object InstitutionPicker : NoArgumentsDestination(Pane.INSTITUTION_PICKER.name)

    object Consent : NoArgumentsDestination(Pane.CONSENT.name)

    object PartnerAuth : NoArgumentsDestination(Pane.PARTNER_AUTH.name)

    object AccountPicker : NoArgumentsDestination(Pane.ACCOUNT_PICKER.name)

    object Success : NoArgumentsDestination(Pane.SUCCESS.name)

    object ManualEntry : NoArgumentsDestination(Pane.MANUAL_ENTRY.name)

    object AttachLinkedPaymentAccount :
        NoArgumentsDestination(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT.name)

    object NetworkingLinkSignup : NoArgumentsDestination(Pane.NETWORKING_LINK_SIGNUP_PANE.name)

    object NetworkingLinkLoginWarmup :
        NoArgumentsDestination(Pane.NETWORKING_LINK_LOGIN_WARMUP.name)

    object NetworkingLinkVerification :
        NoArgumentsDestination(Pane.NETWORKING_LINK_VERIFICATION.name)

    object NetworkingSaveToLinkVerification :
        NoArgumentsDestination(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION.name)

    object LinkAccountPicker : NoArgumentsDestination(Pane.LINK_ACCOUNT_PICKER.name)

    object LinkStepUpVerification : NoArgumentsDestination(Pane.LINK_STEP_UP_VERIFICATION.name)

    object Reset : NoArgumentsDestination(Pane.RESET.name)

    object ManualEntrySuccess : Destination(
        route = Pane.MANUAL_ENTRY_SUCCESS.name,
        KEY_MICRODEPOSITS,
        KEY_LAST4
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
                ?.let { MicrodepositVerificationMethod.fromValue(it) }
                ?: MicrodepositVerificationMethod.UNKNOWN

        fun last4(backStackEntry: NavBackStackEntry): String? =
            backStackEntry.arguments?.getString(KEY_LAST4)

        override fun invoke(args: Map<String, String?>): String = route.appendParams(
            KEY_MICRODEPOSITS to args[KEY_MICRODEPOSITS],
            KEY_LAST4 to args[KEY_LAST4]
        )
    }

    companion object {
        const val KEY_MICRODEPOSITS = "microdeposits"
        const val KEY_LAST4 = "last4"
    }

}

internal fun String.appendParams(vararg params: Pair<String, Any?>): String {
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
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = destination.fullRoute,
        arguments = arguments,
        deepLinks = deepLinks,
        content = content
    )
}