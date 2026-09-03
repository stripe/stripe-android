package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultEmbeddedSavedPaymentMethodSelectionHandlerTest {

    @Test
    fun `selection is committed before immediate action`() = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val immediateActions = Turbine<PaymentSelection?>()
        val handler = DefaultEmbeddedSavedPaymentMethodSelectionHandler(
            selectionHolder = selectionHolder,
            immediateActionHandler = {
                immediateActions.add(selectionHolder.selection.value)
            },
        )
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        handler.select(selection)

        assertThat(immediateActions.awaitItem()).isEqualTo(selection)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        immediateActions.ensureAllEventsConsumed()
    }
}
