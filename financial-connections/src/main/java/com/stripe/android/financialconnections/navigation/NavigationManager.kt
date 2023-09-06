package com.stripe.android.financialconnections.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

internal interface NavigationManager {
    val navigationChannel: Channel<NavigationIntent>

    fun tryNavigateTo(
        route: String,
        popUpToCurrent: Boolean = false,
        inclusive: Boolean = false,
        isSingleTop: Boolean = false,
    )
}

internal sealed class NavigationIntent {
    data class NavigateBack(
        val route: String? = null,
        val inclusive: Boolean = false,
    ) : NavigationIntent()

    data class NavigateTo(
        val route: String,
        val popUpToCurrent: Boolean = false,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = true,
    ) : NavigationIntent()
}

internal class NavigationManagerImpl @Inject constructor() : NavigationManager {
    override val navigationChannel = Channel<NavigationIntent>(
        capacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    override fun tryNavigateTo(
        route: String,
        popUpToCurrent: Boolean,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        navigationChannel.trySend(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToCurrent = popUpToCurrent,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }
}
