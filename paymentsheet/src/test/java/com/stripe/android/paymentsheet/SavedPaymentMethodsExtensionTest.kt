package com.stripe.android.paymentsheet

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
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
    fun `uses metadata link brand when available`() {
        val paymentMethod = PaymentMethodFactory.card("pm_123")
        val metadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(linkBrand = LinkBrand.Notlink),
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            )
        )

        val actual = paymentMethod.toDisplayableSavedPaymentMethod(
            paymentMethodMetadata = metadata,
            defaultPaymentMethodId = null,
        )

        assertEquals(actual.linkBrand, LinkBrand.Notlink)
    }

    private fun testSetup(paymentMethodId: String, defaultPaymentMethodId: String?): DisplayableSavedPaymentMethod {
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        return paymentMethod.toDisplayableSavedPaymentMethod(
            paymentMethodMetadata = null,
            defaultPaymentMethodId = defaultPaymentMethodId
        )
    }
}
