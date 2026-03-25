package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import com.stripe.android.paymentsheet.R as PaymentSheetR

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

    private val _screen = MutableStateFlow(initialScreen)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _result = MutableSharedFlow<TapToAddResult>(replay = 1)
    val result: SharedFlow<TapToAddResult> = _result.asSharedFlow()

    fun performAction(action: Action) {
        _screen.value.close()

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
            Action.CloseWithUnsupportedDevice -> {
                coroutineScope.launch {
                    _result.emit(TapToAddResult.UnsupportedDevice)
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
                _screen.value = action.screen
            }
        }
    }

    sealed class Screen : Closeable {
        abstract val cancelButton: CancelButton
        abstract val onCancelAction: Action

        @Composable
        protected abstract fun ColumnScope.Content()

        override fun close() {
            // No-op
        }

        @Composable
        fun ScreenContent(scope: ColumnScope) {
            scope.Content()
        }

        data class Collecting(
            val interactor: TapToAddCollectingInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.None
            override val onCancelAction: Action = Action.Close

            @Composable
            override fun ColumnScope.Content() {
                TapToAddCollectingScreen()
            }

            override fun close() {
                interactor.close()
            }
        }

        data class CardAdded(
            val interactor: TapToAddCardAddedInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible
            override val onCancelAction: Action = Action.Close

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

            override fun close() {
                interactor.close()
            }
        }

        data class Delay(
            val interactor: TapToAddDelayInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible
            override val onCancelAction: Action = Action.Close

            @Composable
            override fun ColumnScope.Content() {
                TapToAddDelayScreen(
                    cardBrand = interactor.cardBrand,
                    last4 = interactor.last4,
                )
            }

            override fun close() {
                interactor.close()
            }
        }

        data class Confirmation(
            val interactor: TapToAddConfirmationInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible
            override val onCancelAction: Action = Action.Close

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

            override fun close() {
                interactor.close()
            }
        }

        data class Error(
            val message: ResolvableString,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible
            override val onCancelAction: Action = Action.Close

            @Composable
            override fun ColumnScope.Content() {
                TapToAddErrorScreen(message)
            }
        }

        data object NotSupportedError : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible
            override val onCancelAction: Action = Action.CloseWithUnsupportedDevice

            @Composable
            override fun ColumnScope.Content() {
                TapToAddErrorScreen(
                    PaymentSheetR.string.stripe_tap_to_add_unsupported_device_error.resolvableString
                )
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
        data object CloseWithUnsupportedDevice : Action
        data object Complete : Action
        data class Continue(val paymentSelection: PaymentSelection.Saved) : Action
    }
}
