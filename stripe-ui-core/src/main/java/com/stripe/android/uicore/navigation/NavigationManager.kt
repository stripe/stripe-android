package com.stripe.android.uicore.navigation

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface NavigationManager {
    val navigationFlow: SharedFlow<NavigationIntent>

    fun tryNavigateTo(
        route: String,
        popUpTo: PopUpToBehavior? = null,
        isSingleTop: Boolean = true,
    )

    fun tryNavigateBack()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PopUpToBehavior {
    val inclusive: Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Start : PopUpToBehavior {
        override val inclusive = true
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Current(
        override val inclusive: Boolean,
    ) : PopUpToBehavior

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Route(
        override val inclusive: Boolean,
        val route: String,
    ) : PopUpToBehavior
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class NavigationIntent {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class NavigateTo(
        val route: String,
        val popUpTo: PopUpToBehavior?,
        val isSingleTop: Boolean,
    ) : NavigationIntent()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object NavigateBack : NavigationIntent()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NavigationManagerImpl @Inject constructor() : NavigationManager {
    private val _navigationFlow = MutableSharedFlow<NavigationIntent>(extraBufferCapacity = 1)

    override val navigationFlow = _navigationFlow.asSharedFlow()

    override fun tryNavigateTo(
        route: String,
        popUpTo: PopUpToBehavior?,
        isSingleTop: Boolean,
    ) {
        _navigationFlow.tryEmit(
            NavigationIntent.NavigateTo(
                route = route,
                popUpTo = popUpTo,
                isSingleTop = isSingleTop,
            )
        )
    }

    override fun tryNavigateBack() {
        _navigationFlow.tryEmit(NavigationIntent.NavigateBack)
    }
}
