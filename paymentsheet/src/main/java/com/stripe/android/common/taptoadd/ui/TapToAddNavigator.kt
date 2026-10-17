package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.taptoadd.TapToAddErrorMessage
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
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
                action.preCloseAction()

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
        abstract val cancelButton: StateFlow<CancelButton>

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
            override val cancelButton: StateFlow<CancelButton> = stateFlowOf(CancelButton.None)

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
            override val cancelButton: StateFlow<CancelButton> = interactor.state.mapAsStateFlow { state ->
                state.primaryButton?.let {
                    CancelButton.Available(
                        action = Action.Close {
                            interactor.performAction(TapToAddCardAddedInteractor.Action.CancelPressed)
                        }
                    )
                } ?: run {
                    CancelButton.Invisible
                }
            }

            @Composable
            override fun ColumnScope.Content() {
                val state by interactor.state.collectAsState()

                TapToAddCardAddedScreen(
                    state = state,
                    onPrimaryButtonPress = {
                        interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)
                    },
                    onScreenShown = {
                        interactor.performAction(TapToAddCardAddedInteractor.Action.ScreenShown)
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
            override val cancelButton: StateFlow<CancelButton> = stateFlowOf(
                CancelButton.Available(
                    action = Action.Close()
                )
            )

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
            override val cancelButton: StateFlow<CancelButton> = interactor.state.mapAsStateFlow { state ->
                when (state.primaryButton.state) {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle -> {
                        CancelButton.Available(
                            action = Action.Close {
                                interactor.performAction(TapToAddConfirmationInteractor.Action.CancelPressed)
                            }
                        )
                    }
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing,
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Success -> {
                        CancelButton.Invisible
                    }
                }
            }

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
            val message: TapToAddErrorMessage,
            val dueToUnsupportedDevice: Boolean,
        ) : Screen() {
            override val cancelButton: StateFlow<CancelButton> = stateFlowOf(
                CancelButton.Available(
                    action = if (dueToUnsupportedDevice) {
                        Action.CloseWithUnsupportedDevice
                    } else {
                        Action.Close()
                    }
                )
            )

            @Composable
            override fun ColumnScope.Content() {
                TapToAddErrorScreen(
                    title = message.title,
                    action = message.action,
                )
            }
        }
    }

    sealed interface CancelButton {
        data object None : CancelButton
        data object Invisible : CancelButton
        class Available(val action: Action) : CancelButton
    }

    sealed interface Action {
        class NavigateTo(val screen: Screen) : Action
        class Close(val preCloseAction: () -> Unit = {}) : Action
        data object CloseWithUnsupportedDevice : Action
        data object Complete : Action
        data class Continue(val paymentSelection: PaymentSelection.Saved) : Action
    }
}
