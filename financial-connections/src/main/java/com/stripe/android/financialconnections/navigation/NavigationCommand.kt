package com.stripe.android.financialconnections.navigation

import android.util.Log
import androidx.navigation.NamedNavArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

interface NavigationCommand {
    val arguments: List<NamedNavArgument>
    val destination: String
}

object NavigationDirections {

    val bankPicker = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "bank-picker"
    }

    val consent = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = "bank-intro"
    }

    val Default = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val destination = ""
    }
}

class NavigationManager(private val externalScope: CoroutineScope) {
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
