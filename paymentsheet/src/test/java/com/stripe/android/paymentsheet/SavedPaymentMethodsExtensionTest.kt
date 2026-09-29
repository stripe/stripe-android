package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedPaymentMethodsExtensionTest {
    @Test
    fun `Pending selectionState marks only the matching payment method pending`() {
        val selectionState = SavedPaymentMethodSelectionState.Pending("aaa111")

        assertTrue(testSetup("aaa111", selectionState).isSelectionPending)
        assertFalse(testSetup("bbb222", selectionState).isSelectionPending)
    }

    private fun testSetup(
        paymentMethodId: String,
        selectionState: SavedPaymentMethodSelectionState,
    ): DisplayableSavedPaymentMethod {
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        return paymentMethod.toDisplayableSavedPaymentMethod(
            paymentMethodMetadata = null,
            defaultPaymentMethodId = null,
            selectionState = selectionState,
        )
    }
}
