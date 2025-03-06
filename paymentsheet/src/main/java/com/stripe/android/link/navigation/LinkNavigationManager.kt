package com.stripe.android.link.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.ScreenState
import com.stripe.android.link.injection.NativeLinkScope
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@NativeLinkScope
internal class LinkNavigationManager @Inject constructor(
    private val navigationManager: NavigationManager,
) {

    val events: SharedFlow<NavigationIntent> = navigationManager.navigationFlow

    private val _linkScreenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val linkScreenState: StateFlow<ScreenState> = _linkScreenState.asStateFlow()

    fun handleVerificationSuccess() {
        if (linkScreenState.value is ScreenState.VerificationDialog) {
            _linkScreenState.value = ScreenState.FullScreen
        } else {
            navigationManager.tryNavigateTo(
                route = LinkScreen.Wallet.route,
                popUpTo = PopUpToBehavior.Current(inclusive = true),
            )
        }
    }

    fun updateScreenState(screenState: ScreenState) {
        _linkScreenState.value = screenState
    }

    fun tryNavigateTo(
        route: String,
        popUpTo: PopUpToBehavior? = null,
        isSingleTop: Boolean = true,
    ) {
        navigationManager.tryNavigateTo(route, popUpTo, isSingleTop)
    }

    fun tryNavigateBack() {
        navigationManager.tryNavigateBack()
    }
}

@Composable
internal fun NavHostController.previousBackStackEntryAsState(): State<NavBackStackEntry?> {
    val state = remember { mutableStateOf<NavBackStackEntry?>(null) }

    val listener = remember {
        NavController.OnDestinationChangedListener { controller, _, _ ->
            state.value = controller.previousBackStackEntry
        }
    }

    DisposableEffect(Unit) {
        addOnDestinationChangedListener(listener)
        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }

    return state
}
