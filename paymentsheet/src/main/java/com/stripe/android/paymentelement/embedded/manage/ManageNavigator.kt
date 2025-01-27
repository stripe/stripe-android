package com.stripe.android.paymentelement.embedded.manage

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

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

    private val _result = MutableSharedFlow<Result>(replay = 1)
    val result: SharedFlow<Result> = _result.asSharedFlow()

    fun performAction(action: Action) {
        when (action) {
            is Action.Back -> {
                if (navigationHandler.canGoBack) {
                    navigationHandler.pop()
                } else {
                    _result.tryEmit(Result(maintainPaymentSelection = false))
                }
            }
            is Action.Close -> {
                _result.tryEmit(Result(maintainPaymentSelection = true))
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
            private val manageNavigator: () -> ManageNavigator,
        ) : Screen() {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(null)

            override fun title(): StateFlow<ResolvableString?> {
                return stateFlowOf(null)
            }

            @Composable
            override fun Content() {
                Column {
                    Text("All Screen")
                    Button(
                        onClick = {
                            manageNavigator().performAction(Action.Close)
                        }
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            manageNavigator().performAction(Action.GoToScreen(Update(manageNavigator)))
                        }
                    ) {
                        Text("Update Screen")
                    }
                }
            }
        }

        class Update(
            private val manageNavigator: () -> ManageNavigator,
        ) : Screen() {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(null)

            override fun title(): StateFlow<ResolvableString?> {
                return stateFlowOf(null)
            }

            @Composable
            override fun Content() {
                Column {
                    Text("Update Screen.")
                    Button(
                        onClick = {
                            manageNavigator().performAction(Action.Back)
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }

    sealed class Action {
        object Back : Action()

        object Close : Action()

        data class GoToScreen(val screen: Screen) : Action()
    }

    data class Result(
        val maintainPaymentSelection: Boolean,
    )
}
