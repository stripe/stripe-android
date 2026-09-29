package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedPaymentMethodsExtensionTest {
    @Test
    fun `shouldShowDefaultBadge is false when defaultPaymentMethodId is null and paymentMethod not null`() {
        val paymentMethodId = "aaa111"
        val defaultPaymentMethodId = null

        val actual = testSetup(paymentMethodId, defaultPaymentMethodId)
        assertEquals(actual.shouldShowDefaultBadge, false)
    }

    @Test
    fun `shouldShowDefaultBadge false when defaultPaymentMethodId != paymentMethod id and both not null`() {
        val paymentMethodId = "aaa111"
        val defaultPaymentMethodId = "bbb222"

        val actual = testSetup(paymentMethodId, defaultPaymentMethodId)
        assertEquals(actual.shouldShowDefaultBadge, false)
    }

    @Test
    fun `shouldShowDefaultBadge is false when defaultPaymentMethodId is null`() {
        val actual = testSetup(paymentMethodId = "pm_123", null)
        assertEquals(actual.shouldShowDefaultBadge, false)
    }

    @Test
    fun `shouldShowDefaultBadge is true when defaultPaymentMethodId == paymentMethod id and both are not null`() {
        val actual = testSetup("aaa111", "aaa111")
        assertEquals(actual.shouldShowDefaultBadge, true)
    }

    @Test
    fun `Pending selectionState marks only the matching payment method pending`() {
        val selectionState = SavedPaymentMethodSelectionState.Pending("aaa111")

        assertTrue(testSetup("aaa111", null, selectionState).isSelectionPending)
        assertFalse(testSetup("bbb222", null, selectionState).isSelectionPending)
    }

    private fun testSetup(
        paymentMethodId: String,
        defaultPaymentMethodId: String?,
        selectionState: SavedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
    ): DisplayableSavedPaymentMethod {
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        return paymentMethod.toDisplayableSavedPaymentMethod(
            paymentMethodMetadata = null,
            defaultPaymentMethodId = defaultPaymentMethodId,
            selectionState = selectionState,
        )
    }
}
