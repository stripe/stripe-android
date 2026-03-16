package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.uicore.utils.collectAsState
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
    private val stateHolder: TapToAddStateHolder,
    initialScreen: Screen,
) {
    @Inject constructor(
        @ViewModelScope coroutineScope: CoroutineScope,
        initialTapToAddScreenFactory: InitialTapToAddScreenFactory,
        stateHolder: TapToAddStateHolder,
    ) : this(
        coroutineScope = coroutineScope,
        initialScreen = initialTapToAddScreenFactory.createInitialScreen(),
        stateHolder = stateHolder,
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
                    val paymentSelection = when (val state = stateHolder.state) {
                        is TapToAddStateHolder.State.CardAdded -> {
                            PaymentSelection.Saved(paymentMethod = state.paymentMethod)
                        }
                        is TapToAddStateHolder.State.Confirmation -> {
                            PaymentSelection.Saved(
                                paymentMethod = state.paymentMethod,
                                linkInput = state.linkInput,
                            )
                        }
                        else -> null
                    }

                    _result.emit(TapToAddResult.Canceled(paymentSelection = paymentSelection))
                }
            }
            is Action.Complete -> {
                coroutineScope.launch {
                    _result.emit(TapToAddResult.Complete)
                }
            }
            is Action.Continue -> {
                coroutineScope.launch {
                    _result.emit(
                        TapToAddResult.Continue(
                            paymentSelection = action.paymentSelection
                        )
                    )
                }
            }
            is Action.NavigateTo -> {
                navigationHandler.transitionTo(action.screen)
            }
        }
    }

    sealed class Screen {
        abstract val cancelButton: CancelButton

        @Composable
        protected abstract fun ColumnScope.Content()

        @Composable
        fun ScreenContent(scope: ColumnScope) {
            scope.Content()
        }

        data class Collecting(
            val interactor: TapToAddCollectingInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.None

            @Composable
            override fun ColumnScope.Content() {
                TapToAddCollectingScreen()
            }
        }

        data class CardAdded(
            val interactor: TapToAddCardAddedInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun ColumnScope.Content() {
                val state by interactor.state.collectAsState()

                TapToAddCardAddedScreen(
                    state = state,
                    onPrimaryButtonPress = {
                        interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)
                    }
                )
            }
        }

        data class Delay(
            val interactor: TapToAddDelayInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun ColumnScope.Content() {
                TapToAddDelayScreen(
                    cardBrand = interactor.cardBrand,
                    last4 = interactor.last4,
                )
            }
        }

        data class Confirmation(
            val interactor: TapToAddConfirmationInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun ColumnScope.Content() {
                val state by interactor.state.collectAsState()

                TapToAddConfirmationScreen(
                    state = state,
                    onPrimaryButtonPress = {
                        interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)
                    },
                    onProcessingComplete = {
                        interactor.performAction(TapToAddConfirmationInteractor.Action.SuccessShown)
                    }
                )
            }
        }

        data class Error(
            val message: ResolvableString,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun ColumnScope.Content() {
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
        data object Complete : Action
        data class Continue(val paymentSelection: PaymentSelection.Saved) : Action
    }
}
