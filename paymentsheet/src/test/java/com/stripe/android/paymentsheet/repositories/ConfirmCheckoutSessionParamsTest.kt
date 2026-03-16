package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import kotlin.test.Test

class ConfirmCheckoutSessionParamsTest {

    @Test
    fun `toParamMap includes shared fields`() {
        val params = createParams().toParamMap()

        assertThat(params["payment_method"]).isEqualTo("pm_test_123")
        assertThat(params["return_url"]).isEqualTo("stripesdk://return_url")
        assertThat(params).containsKey("client_attribution_metadata")
    }

    @Test
    fun `toParamMap omits expectedAmount when null`() {
        val params = createParams(expectedAmount = null).toParamMap()

        assertThat(params).doesNotContainKey("expected_amount")
    }

    @Test
    fun `toParamMap includes expectedAmount when set`() {
        val params = createParams(expectedAmount = 5099L).toParamMap()

        assertThat(params["expected_amount"]).isEqualTo(5099L)
    }

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

    private fun createParams(
        expectedAmount: Long? = null,
        savePaymentMethod: Boolean? = null,
    ): ConfirmCheckoutSessionParams {
        return ConfirmCheckoutSessionParams(
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
