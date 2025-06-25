package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.shoppay.ShopPayArgs
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultBridgeHandlerTest {

    @Test
    fun `getStripePublishableKey returns the publishable key from ShopPayArgs`() {
        val handler = createDefaultBridgeHandler()

        val actualKey = handler.getStripePublishableKey()

        assertThat(actualKey).isEqualTo(PUBLISHABLE_KEY)
    }

    @Test
    fun `handleECEClick returns success response when parsing succeeds`() {
        val fakeParser = FakeHandleClickRequestParser(
            returnValue = HandleClickRequest(
                eventData = HandleClickRequest.EventData(
                    expressPaymentType = "shopPay"
                )
            )
        )
        val handler = createDefaultBridgeHandler(handleClickRequestJsonParser = fakeParser)
        val message = """{"eventData": {"expressPaymentType": "shopPay"}}"""

        val result = handler.handleECEClick(message)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("data")

        val data = response.getJSONObject("data")
        assertThat(data.getJSONObject("business").getString("name")).isEqualTo("Test Business")
        assertThat(data.getString("shopId")).isEqualTo(SHOP_PAY_CONFIGURATION.shopId)
        assertThat(data.getBoolean("phoneNumberRequired")).isTrue()
        assertThat(data.getJSONArray("allowedShippingCountries").length()).isEqualTo(2)
    }

    @Test
    fun `handleECEClick returns error response when parsing fails`() {
        val fakeParser = FakeHandleClickRequestParser(returnValue = null)
        val handler = createDefaultBridgeHandler(handleClickRequestJsonParser = fakeParser)
        val message = """{"invalid": "json"}"""

        val result = handler.handleECEClick(message)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Failed to parse handle click request")
    }

    @Test
    fun `handleECEClick returns error response when JSON parsing throws exception`() {
        val fakeParser = FakeHandleClickRequestParser(returnValue = null)
        val handler = createDefaultBridgeHandler(handleClickRequestJsonParser = fakeParser)
        val invalidJson = "invalid json"

        val result = handler.handleECEClick(invalidJson)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Error parsing handle click request")
    }

    private fun createDefaultBridgeHandler(
        handleClickRequestJsonParser: ModelJsonParser<HandleClickRequest> = FakeHandleClickRequestParser()
    ): DefaultBridgeHandler {
        val shopPayArgs = ShopPayArgs(
            publishableKey = PUBLISHABLE_KEY,
            shopPayConfiguration = SHOP_PAY_CONFIGURATION,
            customerSessionClientSecret = "css_test_123",
            businessName = "Test Business",
            paymentElementCallbackIdentifier = "paymentElementCallbackIdentifier"
        )

        return DefaultBridgeHandler(
            handleClickRequestJsonParser = handleClickRequestJsonParser,
            shopPayArgs = shopPayArgs
        )
    }

    private class FakeHandleClickRequestParser(
        private val returnValue: HandleClickRequest? = null
    ) : ModelJsonParser<HandleClickRequest> {
        override fun parse(json: JSONObject): HandleClickRequest? = returnValue
    }

    companion object {
        const val PUBLISHABLE_KEY = "pk_test_123"
    }
}
