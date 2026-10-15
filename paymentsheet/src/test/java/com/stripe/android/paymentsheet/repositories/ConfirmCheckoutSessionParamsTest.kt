package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodSelectionFlow
import kotlin.test.Test

class ConfirmCheckoutSessionParamsTest {

    @Test
    fun `serializes collected information email`() {
        val params = createParams(
            collectedInformation = ConfirmCheckoutSessionParams.CollectedInformation(email = "  email@example.com  "),
        ).toParamMap()
        assertThat(params["collected_information"]).isEqualTo(mapOf("email" to "  email@example.com  "))
    }

    @Test
    fun `omits collected information when absent`() {
        assertThat(createParams().toParamMap()).doesNotContainKey("collected_information")
    }

    @Test
    fun `toParamMap includes shared fields`() {
        val params = createParams().toParamMap()

        assertThat(params["payment_method"]).isEqualTo("pm_test_123")
        assertThat(params["expected_payment_method_type"]).isEqualTo("card")
        assertThat(params["return_url"]).isEqualTo("stripesdk://return_url")
        assertThat(params).containsKey("client_attribution_metadata")
    }

    @Test
    fun `serializes Link payment method type`() {
        val params = createParams().copy(expectedPaymentMethodType = PaymentMethod.Type.Link.code).toParamMap()

        assertThat(params["expected_payment_method_type"]).isEqualTo("link")
    }

    @Test
    fun `omits expected payment method type when unavailable`() {
        val params = createParams().copy(expectedPaymentMethodType = null).toParamMap()

        assertThat(params).doesNotContainKey("expected_payment_method_type")
    }

    @Test
    fun `toParamMap includes expectedAmount when zero`() {
        val params = createParams(expectedAmount = 0L).toParamMap()

        assertThat(params["expected_amount"]).isEqualTo(0L)
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

    @Test
    fun `toParamMap includes shipping information when set`() {
        val params = createParams(shipping = SHIPPING).toParamMap()

        assertThat(params["shipping"]).isEqualTo(
            mapOf(
                "name" to "Jane Doe",
                "address" to mapOf(
                    "line1" to "354 Oyster Point Blvd",
                    "line2" to "Suite 200",
                    "city" to "South San Francisco",
                    "state" to "CA",
                    "postal_code" to "94080",
                    "country" to "US",
                ),
            )
        )
    }

    @Test
    fun `toParamMap omits shipping information when null`() {
        val params = createParams(shipping = null).toParamMap()

        assertThat(params).doesNotContainKey("shipping")
    }

    private fun createParams(
        expectedAmount: Long = 5099L,
        savePaymentMethod: Boolean? = null,
        shipping: ConfirmCheckoutSessionParams.Shipping? = null,
        collectedInformation: ConfirmCheckoutSessionParams.CollectedInformation? = null,
    ): ConfirmCheckoutSessionParams {
        return ConfirmCheckoutSessionParams(
            collectedInformation = collectedInformation,
            paymentMethodId = "pm_test_123",
            expectedPaymentMethodType = PaymentMethod.Type.Card.code,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
            returnUrl = "stripesdk://return_url",
            expectedAmount = expectedAmount,
            savePaymentMethod = savePaymentMethod,
            shipping = shipping,
        )
    }

    private companion object {
        val CLIENT_ATTRIBUTION_METADATA = ClientAttributionMetadata(
            elementsSessionConfigId = "test_session_id",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            checkoutSessionId = null,
        )

        val SHIPPING = ConfirmCheckoutSessionParams.Shipping(
            name = "Jane Doe",
            address = Address(
                line1 = "354 Oyster Point Blvd",
                line2 = "Suite 200",
                city = "South San Francisco",
                state = "CA",
                postalCode = "94080",
                country = "US",
            ),
        )
    }
}
