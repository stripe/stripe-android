package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `Idle selectionState marks nothing pending or failed`() {
        val actual = testSetup(
            paymentMethodId = "aaa111",
            defaultPaymentMethodId = null,
            selectionState = SavedPaymentMethodSelectionState.Idle,
        )

        assertFalse(actual.isSelectionPending)
        assertNull(actual.selectionError)
    }

    @Test
    fun `Pending selectionState for the same payment method marks it pending`() {
        val actual = testSetup(
            paymentMethodId = "aaa111",
            defaultPaymentMethodId = null,
            selectionState = SavedPaymentMethodSelectionState.Pending("aaa111"),
        )

        assertTrue(actual.isSelectionPending)
        assertNull(actual.selectionError)
    }

    @Test
    fun `Pending selectionState for another payment method does not mark it pending`() {
        val actual = testSetup(
            paymentMethodId = "aaa111",
            defaultPaymentMethodId = null,
            selectionState = SavedPaymentMethodSelectionState.Pending("bbb222"),
        )

        assertFalse(actual.isSelectionPending)
        assertNull(actual.selectionError)
    }

    @Test
    fun `Failed selectionState for the same payment method sets selectionError`() {
        val error = "Something went wrong".resolvableString

        val actual = testSetup(
            paymentMethodId = "aaa111",
            defaultPaymentMethodId = null,
            selectionState = SavedPaymentMethodSelectionState.Failed("aaa111", error),
        )

        assertFalse(actual.isSelectionPending)
        assertEquals(actual.selectionError, error)
    }

    @Test
    fun `Failed selectionState for another payment method does not set selectionError`() {
        val error = "Something went wrong".resolvableString

        val actual = testSetup(
            paymentMethodId = "aaa111",
            defaultPaymentMethodId = null,
            selectionState = SavedPaymentMethodSelectionState.Failed("bbb222", error),
        )

        assertNull(actual.selectionError)
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
