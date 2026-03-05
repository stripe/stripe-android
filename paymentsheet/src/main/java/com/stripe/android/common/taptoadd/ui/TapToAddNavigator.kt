package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
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
    private val paymentMethodHolder: TapToAddPaymentMethodHolder,
    initialScreen: Screen,
) {
    @Inject constructor(
        @ViewModelScope coroutineScope: CoroutineScope,
        initialTapToAddScreenFactory: InitialTapToAddScreenFactory,
        paymentMethodHolder: TapToAddPaymentMethodHolder,
    ) : this(
        coroutineScope = coroutineScope,
        initialScreen = initialTapToAddScreenFactory.createInitialScreen(),
        paymentMethodHolder = paymentMethodHolder,
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

    fun navigateToPostCollectionScreen(
        paymentMethod: PaymentMethod,
        paymentMethodMetadata: PaymentMethodMetadata,
        collectCvcInteractorFactory: TapToAddCollectCvcInteractor.Factory,
        confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    ) {
        val screen = if (
            paymentMethodOptionsParams == null &&
            requiresTapToAddCvcCollection(paymentMethodMetadata, paymentMethod)
        ) {
            Screen.CollectCvc(
                interactor = collectCvcInteractorFactory.create(paymentMethod)
            )
        } else {
            Screen.Confirmation(
                interactor = confirmationInteractorFactory.create(
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                )
            )
        }

        performAction(Action.NavigateTo(screen))
    }

    fun performAction(action: Action) {
        when (action) {
            is Action.Close -> {
                coroutineScope.launch {
                    val paymentSelection = paymentMethodHolder.paymentMethod?.let { paymentMethod ->
                        PaymentSelection.Saved(paymentMethod)
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

        data class Completed(
            val interactor: TapToAddCompletedInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Invisible

            @Composable
            override fun ColumnScope.Content() {
                TapToAddCompletedScreen(
                    cardBrand = interactor.cardBrand,
                    last4 = interactor.last4,
                    label = interactor.label,
                )
            }
        }

        data class CollectCvc(
            val interactor: TapToAddCollectCvcInteractor,
        ) : Screen() {
            override val cancelButton: CancelButton = CancelButton.Visible

            @Composable
            override fun ColumnScope.Content() {
                val state by interactor.state.collectAsState()

                TapToAddCollectCvcScreen(
                    state = state,
                    onPrimaryButtonPress = {
                        interactor.performAction(TapToAddCollectCvcInteractor.Action.PrimaryButtonPressed)
                    }
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
