package com.stripe.android.financialconnections.navigation

import android.util.Log
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.NavType.EnumType
import androidx.navigation.navArgument
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal interface NavigationCommand {
    val arguments: List<NamedNavArgument>
    val destination: String
}

internal object NavigationDirections {

    val institutionPicker = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "bank-picker"
    }

    val consent = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "bank-intro"
    }

    val partnerAuth = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "partner-auth"
    }

    val accountPicker = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "account-picker"
    }

    val success = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "success"
    }

    val manualEntry = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "manual_entry"
    }

    val attachLinkedPaymentAccount = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "attach_linked_payment_account"
    }

    val networkingLinkSignup = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "networking_link_signup_pane"
    }

    val networkingLinkLoginWarmup = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "networking_link_login_warmup"
    }

    val networkingLinkVerification = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "networking_link_verification_pane"
    }

    val linkAccountPicker = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "linkaccount_picker"
    }

    val reset = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "reset"
    }

    object ManualEntrySuccess {

        private const val KEY_MICRODEPOSITS = "microdeposits"
        private const val KEY_LAST4 = "last4"
        const val route = "manual_entry_success?" +
            "$KEY_MICRODEPOSITS={$KEY_MICRODEPOSITS}," +
            "$KEY_LAST4={$KEY_LAST4}"

        val arguments = listOf(
            navArgument(KEY_LAST4) { type = NavType.StringType },
            navArgument(KEY_MICRODEPOSITS) { type = EnumType(MicrodepositVerificationMethod::class.java) }
        )

        fun argMap(
            microdepositVerificationMethod: MicrodepositVerificationMethod,
            last4: String?
        ): Map<String, Any?> = mapOf(
            KEY_MICRODEPOSITS to microdepositVerificationMethod,
            KEY_LAST4 to last4
        )

        fun microdeposits(backStackEntry: NavBackStackEntry): MicrodepositVerificationMethod =
            backStackEntry.arguments?.getSerializable(KEY_MICRODEPOSITS)
                as MicrodepositVerificationMethod

        fun last4(backStackEntry: NavBackStackEntry): String? =
            backStackEntry.arguments?.getString(KEY_LAST4)
        operator fun invoke(args: Map<String, Any?>) = object : NavigationCommand {
            override val arguments = ManualEntrySuccess.arguments
            val last4 = args.getValue(KEY_LAST4) as? String
            val microdeposits = args.getValue(KEY_MICRODEPOSITS) as? MicrodepositVerificationMethod
            override val destination = "manual_entry_success?" +
                "$KEY_MICRODEPOSITS=$microdeposits," +
                "$KEY_LAST4=$last4"
        }
    }

    val Default = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = ""
    }
}

internal class NavigationManager(private val externalScope: CoroutineScope) {
    var commands = MutableSharedFlow<NavigationCommand>()
    fun navigate(
        directions: NavigationCommand
    ) {
        Log.e("NavigationManager", "navigate: $directions")
        externalScope.launch {
            commands.emit(directions)
        }
    }
}
