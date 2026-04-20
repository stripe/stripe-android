package com.stripe.android.paymentelement.embedded.manage

import androidx.activity.result.ActivityResultCaller
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.FormActivitySubcomponent
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.form.FormScreen
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
import kotlinx.coroutines.launch
import java.io.Closeable

internal class EmbeddedNavigator private constructor(
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

    // result value is shouldInvokeRowSelectionCallback
    private val _result = MutableSharedFlow<Boolean?>(replay = 1)
    val result: SharedFlow<Boolean?> = _result.asSharedFlow()

    private var activityContext: ScreenActivityContext? = null

    init {
        onScreenShown(screen.value)
    }

    fun bindToActivity(activityContext: ScreenActivityContext) {
        this.activityContext = activityContext
        screen.value.onActivityReady(activityContext)
    }

    fun performAction(action: Action) {
        when (action) {
            is Action.Back -> {
                onScreenHidden(screen.value)
                if (navigationHandler.canGoBack) {
                    navigationHandler.pop()
                } else {
                    _result.tryEmit(null)
                }
            }
            is Action.Close -> {
                onScreenHidden(screen.value)
                _result.tryEmit(action.shouldInvokeRowSelectionCallback)
            }
            is Action.GoToScreen -> {
                navigationHandler.transitionToWithDelay(action.screen)
                activityContext?.let { action.screen.onActivityReady(it) }
                onScreenShown(action.screen)
            }
        }
    }

    private fun onScreenShown(screen: Screen) {
        when (screen) {
            is Screen.Form -> Unit
            is Screen.All -> eventReporter.onShowManageSavedPaymentMethods()
            is Screen.Update -> eventReporter.onShowEditablePaymentOption()
        }
    }

    private fun onScreenHidden(screen: Screen) {
        when (screen) {
            is Screen.Form -> Unit
            is Screen.All -> Unit
            is Screen.Update -> eventReporter.onHideEditablePaymentOption()
        }
    }

    interface ScreenResultHandler {
        fun onProcessingCompleted() {}
        fun onDismissed() {}
        fun onFormResult(result: FormResult) {}
    }

    data class ScreenActivityContext(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
        val coroutineScope: CoroutineScope,
        val resultHandler: ScreenResultHandler,
    )

    sealed class Screen {
        @Composable
        abstract fun Content()

        abstract fun topBarState(): StateFlow<PaymentSheetTopBarState?>

        abstract fun title(): StateFlow<ResolvableString?>

        abstract fun isPerformingNetworkOperation(): Boolean

        open fun onActivityReady(activityContext: ScreenActivityContext) {}

        class Form(
            private val formActivityStateHelper: FormActivityStateHelper,
            private val subcomponentFactory: FormActivitySubcomponent.Factory,
        ) : Screen() {
            private var initialized: InitializedState? = null

            private class InitializedState(
                val formScreen: FormScreen,
                val onProcessingCompleted: () -> Unit,
                val onDismissed: () -> Unit,
            )

            fun setResult(result: FormResult) {
                formActivityStateHelper.setResult(result)
            }

            override fun onActivityReady(activityContext: ScreenActivityContext) {
                val subcomponent = subcomponentFactory.build(
                    activityResultCaller = activityContext.activityResultCaller,
                    lifecycleOwner = activityContext.lifecycleOwner,
                )
                initialized = InitializedState(
                    formScreen = subcomponent.formScreen,
                    onProcessingCompleted = activityContext.resultHandler::onProcessingCompleted,
                    onDismissed = activityContext.resultHandler::onDismissed,
                )
                activityContext.coroutineScope.launch {
                    formActivityStateHelper.result.collect { result ->
                        activityContext.resultHandler.onFormResult(result)
                    }
                }
            }

            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(null)
            override fun title(): StateFlow<ResolvableString?> = stateFlowOf(null)
            override fun isPerformingNetworkOperation(): Boolean = formActivityStateHelper.state.value.isProcessing

            @Composable
            override fun Content() {
                val state = initialized ?: return
                state.formScreen.ContentWithoutBottomSheet(
                    onProcessingCompleted = state.onProcessingCompleted,
                    onDismissed = state.onDismissed,
                )
            }
        }

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

        data class Close(val shouldInvokeRowSelectionCallback: Boolean = false) : Action()

        data class GoToScreen(val screen: Screen) : Action()
    }
}
