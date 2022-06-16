package com.stripe.android.financialconnections.navigation

import android.util.Log
import androidx.navigation.NamedNavArgument
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
