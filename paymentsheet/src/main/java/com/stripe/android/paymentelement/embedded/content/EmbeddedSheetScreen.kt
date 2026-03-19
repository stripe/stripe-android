package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.FormActivityUI
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

internal sealed interface EmbeddedSheetScreen {
    @Composable
    fun Content()

    fun topBarState(): StateFlow<PaymentSheetTopBarState?>

    fun title(): StateFlow<ResolvableString?>

    fun canDismiss(): Boolean

    class Form(
        val interactor: DefaultVerticalModeFormInteractor,
        val stateHelper: FormActivityStateHelper,
        val confirmationHelper: FormActivityConfirmationHelper,
        val eventReporter: EventReporter,
        val selectionHolder: EmbeddedSelectionHolder,
        val confirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
        private val onProcessingCompleted: () -> Unit,
        private val onDismissed: () -> Unit,
    ) : EmbeddedSheetScreen {

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(null)

        override fun title(): StateFlow<ResolvableString?> = stateFlowOf(null)

        override fun canDismiss(): Boolean {
            return !stateHelper.state.value.isProcessing
        }

        @Composable
        override fun Content() {
            val state by stateHelper.state.collectAsState()
            FormActivityUI(
                interactor = interactor,
                eventReporter = eventReporter,
                onClick = { confirmationHelper.confirm() },
                onProcessingCompleted = onProcessingCompleted,
                state = state,
                onDismissed = onDismissed,
                updateSelection = selectionHolder::set,
                savedPaymentMethodConfirmInteractorFactory = confirmInteractorFactory,
            )
        }
    }

    class ManageAll(
        val interactor: ManageScreenInteractor,
        private val canGoBack: () -> Boolean,
        private val onBack: () -> Unit,
    ) : EmbeddedSheetScreen, Closeable {
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

        override fun canDismiss(): Boolean = true

        @Composable
        override fun Content() {
            val scrollState = rememberScrollState()
            val topBarStateFlow = remember { topBarState() }
            val titleFlow = remember { title() }
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                BottomSheetScaffold(
                    topBar = {
                        val topBarState by topBarStateFlow.collectAsState()
                        PaymentSheetTopBar(
                            state = topBarState,
                            canNavigateBack = canGoBack(),
                            isEnabled = true,
                            handleBackPressed = onBack,
                        )
                    },
                    content = {
                        val horizontalPadding = StripeTheme.getOuterFormInsets()
                        val headerText by titleFlow.collectAsState()
                        headerText?.let { text ->
                            H4Text(
                                text = text.resolve(),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .padding(horizontalPadding),
                            )
                        }
                        Box(modifier = Modifier.animateContentSize()) {
                            Column {
                                ManageScreenUI(interactor = interactor)
                                PaymentSheetContentPadding(subtractingExtraPadding = 12.dp)
                            }
                        }
                    },
                    scrollState = scrollState,
                )
            }
        }

        override fun close() {
            interactor.close()
        }
    }

    class ManageUpdate(
        val interactor: UpdatePaymentMethodInteractor,
        private val canGoBack: () -> Boolean,
        private val onBack: () -> Unit,
    ) : EmbeddedSheetScreen {
        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(interactor.topBarState)

        override fun title(): StateFlow<ResolvableString?> {
            return stateFlowOf(interactor.screenTitle)
        }

        override fun canDismiss(): Boolean {
            return !interactor.state.value.status.isPerformingNetworkOperation
        }

        @Composable
        override fun Content() {
            val scrollState = rememberScrollState()
            val topBarStateFlow = remember { topBarState() }
            val titleFlow = remember { title() }
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                BottomSheetScaffold(
                    topBar = {
                        val topBarState by topBarStateFlow.collectAsState()
                        PaymentSheetTopBar(
                            state = topBarState,
                            canNavigateBack = canGoBack(),
                            isEnabled = true,
                            handleBackPressed = onBack,
                        )
                    },
                    content = {
                        val horizontalPadding = StripeTheme.getOuterFormInsets()
                        val headerText by titleFlow.collectAsState()
                        headerText?.let { text ->
                            H4Text(
                                text = text.resolve(),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .padding(horizontalPadding),
                            )
                        }
                        Box(modifier = Modifier.animateContentSize()) {
                            Column {
                                UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
                                PaymentSheetContentPadding(subtractingExtraPadding = 16.dp)
                            }
                        }
                    },
                    scrollState = scrollState,
                )
            }
        }
    }
}
