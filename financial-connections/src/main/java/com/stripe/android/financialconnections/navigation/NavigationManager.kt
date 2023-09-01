package com.stripe.android.financialconnections.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

internal interface NavigationManager {
    val navigationChannel: Channel<NavigationIntent>

    suspend fun navigateBack(
        route: String? = null,
        inclusive: Boolean = false,
    )

    fun tryNavigateBack(
        route: String? = null,
        inclusive: Boolean = false,
    )

    suspend fun navigateTo(
        route: String,
        popUpToCurrent: Boolean = false,
        inclusive: Boolean = false,
        isSingleTop: Boolean = true,
    )

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

    override suspend fun navigateBack(route: String?, inclusive: Boolean) {
        navigationChannel.send(
            NavigationIntent.NavigateBack(
                route = route,
                inclusive = inclusive
            )
        )
    }

    override fun tryNavigateBack(route: String?, inclusive: Boolean) {
        navigationChannel.trySend(
            NavigationIntent.NavigateBack(
                route = route,
                inclusive = inclusive
            )
        )
    }

    override suspend fun navigateTo(
        route: String,
        popUpToCurrent: Boolean,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        navigationChannel.send(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToCurrent = popUpToCurrent,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }

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