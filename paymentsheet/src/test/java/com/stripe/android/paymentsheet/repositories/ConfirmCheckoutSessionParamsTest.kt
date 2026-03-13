package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import kotlin.test.Test

class ConfirmCheckoutSessionParamsTest {

    @Test
    fun `base toParamMap includes shared fields`() {
        val params = createBaseParams().toParamMap()

        assertThat(params["payment_method"]).isEqualTo("pm_test_123")
        assertThat(params["return_url"]).isEqualTo("stripesdk://return_url")
        assertThat(params).containsKey("client_attribution_metadata")
    }

    @Test
    fun `base toParamMap does not include payment-specific fields`() {
        val params = createBaseParams().toParamMap()

        assertThat(params).doesNotContainKey("expected_amount")
        assertThat(params).doesNotContainKey("save_payment_method")
    }

    @Test
    fun `payment toParamMap includes expectedAmount`() {
        val params = createPaymentParams(expectedAmount = 5099L).toParamMap()

        assertThat(params["expected_amount"]).isEqualTo(5099L)
    }

    @Test
    fun `payment toParamMap includes shared fields`() {
        val params = createPaymentParams().toParamMap()

        assertThat(params["payment_method"]).isEqualTo("pm_test_123")
        assertThat(params["return_url"]).isEqualTo("stripesdk://return_url")
        assertThat(params).containsKey("client_attribution_metadata")
    }

    @Test
    fun `payment toParamMap with savePaymentMethod true includes save_payment_method true`() {
        val params = createPaymentParams(savePaymentMethod = true).toParamMap()

        assertThat(params["save_payment_method"]).isEqualTo(true)
    }

    @Test
    fun `payment toParamMap with savePaymentMethod false includes save_payment_method false`() {
        val params = createPaymentParams(savePaymentMethod = false).toParamMap()

        assertThat(params["save_payment_method"]).isEqualTo(false)
    }

    @Test
    fun `payment toParamMap with savePaymentMethod null omits save_payment_method`() {
        val params = createPaymentParams(savePaymentMethod = null).toParamMap()

        assertThat(params).doesNotContainKey("save_payment_method")
    }

    private fun createBaseParams(): ConfirmCheckoutSessionParams {
        return ConfirmCheckoutSessionParams(
            paymentMethodId = "pm_test_123",
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
            returnUrl = "stripesdk://return_url",
        )
    }

    private fun createPaymentParams(
        expectedAmount: Long = 1000L,
        savePaymentMethod: Boolean? = null,
    ): ConfirmCheckoutSessionPaymentParams {
        return ConfirmCheckoutSessionPaymentParams(
            paymentMethodId = "pm_test_123",
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
            returnUrl = "stripesdk://return_url",
            expectedAmount = expectedAmount,
            savePaymentMethod = savePaymentMethod,
        )
    }

    private companion object {
        val CLIENT_ATTRIBUTION_METADATA = ClientAttributionMetadata(
            elementsSessionConfigId = "test_session_id",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
        )
    }
}
