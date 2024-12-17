package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import kotlin.test.assertEquals

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
    fun `shouldShowDefaultBadge is false when defaultPaymentMethodId is null and paymentMethod id null`() {
        val actual = testSetup(null, null)
        assertEquals(actual.shouldShowDefaultBadge, false)
    }

    @Test
    fun `shouldShowDefaultBadge is true when defaultPaymentMethodId == paymentMethod id and both are not null`() {
        val actual = testSetup("aaa111", "aaa111")
        assertEquals(actual.shouldShowDefaultBadge, true)
    }

    private fun testSetup(paymentMethodId: String?, defaultPaymentMethodId: String?): DisplayableSavedPaymentMethod {
        val resolvableString = "acbde123".resolvableString

        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString = { pmc ->
            resolvableString
        }

        return paymentMethod.toDisplayableSavedPaymentMethod(
            providePaymentMethodName = providePaymentMethodName,
            paymentMethodMetadata = null,
            defaultPaymentMethodId = defaultPaymentMethodId
        )
    }
}
