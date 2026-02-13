package com.stripe.android.common.taptoadd.ui

import androidx.compose.runtime.Composable
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TapToAddNavigator(
    coroutineScope: CoroutineScope,
    initialScreen: Screen,
) {
    @Inject constructor(
        @ViewModelScope coroutineScope: CoroutineScope,
        initialTapToAddScreenFactory: InitialTapToAddScreenFactory,
    ) : this(
        coroutineScope = coroutineScope,
        initialScreen = initialTapToAddScreenFactory.createInitialScreen(),
    )

    private val navigationHandler = NavigationHandler(
        coroutineScope = coroutineScope,
        initialScreen = initialScreen,
        shouldRemoveInitialScreenOnTransition = false,
        poppedScreenHandler = {},
    )

    val screen: StateFlow<Screen> = navigationHandler.currentScreen

    sealed interface Screen {
        @Composable
        fun Content()

        data class Collecting(
            val interactor: TapToAddCollectingInteractor,
        ) : Screen {
            @Composable
            override fun Content() {
                TapToAddCollectingScreen()
            }
        }
    }

    sealed interface Action {
        data object Close : Action
    }
}
