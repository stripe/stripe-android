package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TapToAddNavigator(
    private val coroutineScope: CoroutineScope,
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

    private val _result = MutableSharedFlow<TapToAddResult>(replay = 1)
    val result: SharedFlow<TapToAddResult> = _result.asSharedFlow()

    fun performAction(action: Action) {
        when (action) {
            is Action.Close -> {
                coroutineScope.launch {
                    _result.emit(TapToAddResult.Canceled(paymentSelection = null))
                }
            }
            is Action.NavigateTo -> {
                navigationHandler.transitionTo(action.screen)
            }
        }
    }

    sealed interface Screen {
        val cancelButton: CancelButton

        @Composable
        fun Content()

        data class Collecting(
            val interactor: TapToAddCollectingInteractor,
        ) : Screen {
            override val cancelButton: CancelButton = CancelButton.None

            @Composable
            override fun Content() {
                TapToAddCollectingScreen()
            }
        }

        data object CardAdded : Screen {
            override val cancelButton: CancelButton = CancelButton.None

            @Composable
            override fun Content() {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Under Construction",
                        color = MaterialTheme.colors.onSurface,
                    )
                }
            }
        }

        data class Error(
            val message: ResolvableString,
        ) : Screen {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun Content() {
                TapToAddErrorScreen(message)
            }
        }
    }

    enum class CancelButton {
        // Button is not rendered
        None,

        // Space for button is rendered but not visible
        Invisible,

        // Button is visible
        Visible
    }

    sealed interface Action {
        class NavigateTo(val screen: Screen) : Action
        data object Close : Action
    }
}
