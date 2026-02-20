package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ConfirmCheckoutSessionParamsTest {

    @Test
    fun `toParamMap with savePaymentMethod true includes save_payment_method true`() {
        val params = createParams(savePaymentMethod = true).toParamMap()

        assertThat(params["save_payment_method"]).isEqualTo(true)
    }

    @Test
    fun `toParamMap with savePaymentMethod false includes save_payment_method false`() {
        val params = createParams(savePaymentMethod = false).toParamMap()

        assertThat(params["save_payment_method"]).isEqualTo(false)
    }

    @Test
    fun `toParamMap with savePaymentMethod null omits save_payment_method`() {
        val params = createParams(savePaymentMethod = null).toParamMap()

        assertThat(params).doesNotContainKey("save_payment_method")
    }

    @Test
    fun `toParamMap includes required fields`() {
        val params = createParams(savePaymentMethod = null).toParamMap()

        assertThat(params["payment_method"]).isEqualTo("pm_test_123")
        assertThat(params["return_url"]).isEqualTo("stripesdk://return_url")
        assertThat(params).containsKey("client_attribution_metadata")
    }

    private fun createParams(savePaymentMethod: Boolean?): ConfirmCheckoutSessionParams {
        return ConfirmCheckoutSessionParams(
            checkoutSessionId = "cs_test_123",
            paymentMethodId = "pm_test_123",
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = "test_session_id",
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            ),
            returnUrl = "stripesdk://return_url",
            savePaymentMethod = savePaymentMethod,
        )
    }
}
