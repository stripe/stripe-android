package com.stripe.android.paymentelement.embedded.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
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
    private val navigationHandler: NavigationHandler<Screen>
) {
    constructor(
        coroutineScope: CoroutineScope,
        initialScreen: Screen,
    ) : this(
        NavigationHandler(
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

    fun performAction(action: Action) {
        when (action) {
            is Action.Back -> {
                if (navigationHandler.canGoBack) {
                    navigationHandler.pop()
                } else {
                    _result.tryEmit(Unit)
                }
            }
            is Action.Close -> {
                _result.tryEmit(Unit)
            }
            is Action.GoToScreen -> {
                navigationHandler.transitionToWithDelay(action.screen)
            }
        }
    }

    sealed class Screen {
        @Composable
        abstract fun Content()

        abstract fun topBarState(): StateFlow<PaymentSheetTopBarState?>

        abstract fun title(): StateFlow<ResolvableString?>

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

            @Composable
            override fun Content() {
                ManageScreenUI(interactor = interactor)
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

            @Composable
            override fun Content() {
                UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
            }
        }
    }

    sealed class Action {
        object Back : Action()

        object Close : Action()

        data class GoToScreen(val screen: Screen) : Action()
    }
}
