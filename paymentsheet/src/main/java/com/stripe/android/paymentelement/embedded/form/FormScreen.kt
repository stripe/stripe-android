package com.stripe.android.paymentelement.embedded.form

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import javax.inject.Inject

@FormActivityScope
internal class FormScreen @Inject constructor(
    private val formInteractor: DefaultVerticalModeFormInteractor,
    private val eventReporter: EventReporter,
    private val formActivityStateHelper: FormActivityStateHelper,
    private val confirmationHelper: FormActivityConfirmationHelper,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
) {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Content(onProcessingCompleted: () -> Unit, onDismissed: () -> Unit) {
        val state by formActivityStateHelper.state.collectAsState()
        val bottomSheetState = rememberStripeBottomSheetState(
            confirmValueChange = { !state.isProcessing }
        )
        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = onDismissed,
        ) {
            FormActivityUI(
                interactor = formInteractor,
                eventReporter = eventReporter,
                onClick = {
                    confirmationHelper.confirm()
                },
                onProcessingCompleted = onProcessingCompleted,
                state = state,
                onDismissed = onDismissed,
                updateSelection = embeddedSelectionHolder::set,
                savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
            )
        }
    }
}
