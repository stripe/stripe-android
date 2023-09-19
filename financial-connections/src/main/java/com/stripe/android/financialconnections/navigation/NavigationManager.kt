package com.stripe.android.financialconnections.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

internal interface NavigationManager {
    val navigationFlow: SharedFlow<NavigationIntent>

    fun tryNavigateTo(
        route: String,
        popUpToCurrent: Boolean = false,
        inclusive: Boolean = false,
        isSingleTop: Boolean = true,
    )
}

internal sealed class NavigationIntent {
    data class NavigateTo(
        val route: String,
        val popUpToCurrent: Boolean,
        val inclusive: Boolean,
        val isSingleTop: Boolean,
    ) : NavigationIntent()
}

internal class NavigationManagerImpl @Inject constructor() : NavigationManager {
    private val _navigationFlow = MutableSharedFlow<NavigationIntent>(extraBufferCapacity = 1)

    override val navigationFlow = _navigationFlow.asSharedFlow()

    override fun tryNavigateTo(
        route: String,
        popUpToCurrent: Boolean,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        _navigationFlow.tryEmit(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToCurrent = popUpToCurrent,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }
}
