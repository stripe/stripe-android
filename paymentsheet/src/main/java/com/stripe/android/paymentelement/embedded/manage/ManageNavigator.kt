package com.stripe.android.paymentelement.embedded.manage

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable

internal class ManageNavigator private constructor(
    private val eventReporter: EventReporter,
    private val navigationHandler: NavigationHandler<Screen>
) {
    constructor(
        coroutineScope: CoroutineScope,
        initialScreen: Screen,
        eventReporter: EventReporter,
    ) : this(
        eventReporter = eventReporter,
        navigationHandler = NavigationHandler(
            coroutineScope = coroutineScope,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
            poppedScreenHandler = {},
        )
    )

    val screen: StateFlow<Screen> = navigationHandler.currentScreen
    val canGoBack: Boolean
        get() = navigationHandler.canGoBack

    private val _result = MutableSharedFlow<Unit>(replay = 1)
    val result: SharedFlow<Unit> = _result.asSharedFlow()

    init {
        onScreenShown(screen.value)
    }

    fun performAction(action: Action) {
        when (action) {
            is Action.Back -> {
                onScreenHidden(screen.value)
                if (navigationHandler.canGoBack) {
                    navigationHandler.pop()
                } else {
                    _result.tryEmit(Unit)
                }
            }
            is Action.Close -> {
                onScreenHidden(screen.value)
                _result.tryEmit(Unit)
            }
            is Action.GoToScreen -> {
                navigationHandler.transitionToWithDelay(action.screen)
                onScreenShown(action.screen)
            }
        }
    }

    private fun onScreenShown(screen: Screen) {
        when (screen) {
            is Screen.All -> eventReporter.onShowManageSavedPaymentMethods()
            is Screen.Update -> eventReporter.onShowEditablePaymentOption()
        }
    }

    private fun onScreenHidden(screen: Screen) {
        when (screen) {
            is Screen.All -> Unit
            is Screen.Update -> eventReporter.onHideEditablePaymentOption()
        }
    }

    sealed class Screen {
        @Composable
        abstract fun Content()

        abstract fun topBarState(): StateFlow<PaymentSheetTopBarState?>

        abstract fun title(): StateFlow<ResolvableString?>

        abstract fun isPerformingNetworkOperation(): Boolean

        class All(
            private val interactor: ManageScreenInteractor,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
                return interactor.state.mapAsStateFlow { state ->
                    state.topBarState(interactor)
                }
            }

            override fun title(): StateFlow<ResolvableString?> {
                return interactor.state.mapAsStateFlow { state ->
                    state.title
                }
            }

            override fun isPerformingNetworkOperation(): Boolean {
                return false
            }

            @Composable
            override fun Content() {
                Column {
                    ManageScreenUI(interactor = interactor)
                    PaymentSheetContentPadding(subtractingExtraPadding = 12.dp)
                }
            }

            override fun close() {
                interactor.close()
            }
        }

        class Update(
            private val interactor: UpdatePaymentMethodInteractor,
        ) : Screen() {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(interactor.topBarState)

            override fun title(): StateFlow<ResolvableString?> {
                return stateFlowOf(interactor.screenTitle)
            }

            override fun isPerformingNetworkOperation(): Boolean {
                return interactor.state.value.status.isPerformingNetworkOperation
            }

            @Composable
            override fun Content() {
                Column {
                    UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
                    PaymentSheetContentPadding(subtractingExtraPadding = 16.dp)
                }
            }
        }
    }

    sealed class Action {
        object Back : Action()

        object Close : Action()

        data class GoToScreen(val screen: Screen) : Action()
    }
}
