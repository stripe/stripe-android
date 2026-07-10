package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.junit.Test

/**
 * Focused on the branches not already covered by `DefaultSavedPaymentMethodRepositoryTest`'s
 * network test (which exercises the happy-path serialization end-to-end).
 */
internal class CheckoutSessionUpdatePaymentMethodParamsTest {

    @Test
    fun `hasSupportedUpdates is false when neither expiry nor billing details are present`() {
        assertThat(paramsWith(expiryMonth = null, expiryYear = null, billingDetails = null).hasSupportedUpdates)
            .isFalse()
    }

    @Test
    fun `hasSupportedUpdates is false when only the expiry month is present`() {
        assertThat(paramsWith(expiryMonth = 12, expiryYear = null, billingDetails = null).hasSupportedUpdates)
            .isFalse()
    }

    @Test
    fun `hasSupportedUpdates is true when only expiry is present`() {
        assertThat(paramsWith(expiryMonth = 12, expiryYear = 2030, billingDetails = null).hasSupportedUpdates)
            .isTrue()
    }

    @Test
    fun `hasSupportedUpdates is true when only billing details are present`() {
        assertThat(
            paramsWith(
                expiryMonth = null,
                expiryYear = null,
                billingDetails = PaymentMethod.BillingDetails(name = "Jane Doe"),
            ).hasSupportedUpdates
        ).isTrue()
    }

    @Test
    fun `toParamMap omits expiry details when only the month is present`() {
        val paramMap = paramsWith(expiryMonth = 12, expiryYear = null, billingDetails = null).toParamMap()

        assertThat(paramMap).containsExactly(
            "payment_method_to_update[payment_method_id]", "pm_123",
            "elements_session_client[is_aggregation_expected]", "true",
        )
    }

    private fun paramsWith(
        expiryMonth: Int?,
        expiryYear: Int?,
        billingDetails: PaymentMethod.BillingDetails?,
    ) = CheckoutSessionUpdatePaymentMethodParams(
        paymentMethodId = "pm_123",
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        billingDetails = billingDetails,
    )
}
