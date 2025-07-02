package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.shoppay.ShopPayTestFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ShopPayPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultShopPayBridgeHandlerTest {

    @Test
    fun `getStripePublishableKey returns the publishable key from ShopPayArgs`() {
        val handler = createDefaultBridgeHandler()

        val actualKey = handler.getStripePublishableKey()

        assertThat(actualKey).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.publishableKey)
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

        val allowedShippingCountries = data.getJSONArray("allowedShippingCountries")
        for (i in 0 until allowedShippingCountries.length()) {
            assertThat(allowedShippingCountries[i])
                .isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.shopPayConfiguration.allowedShippingCountries[i])
        }
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
        val invalidJson = """{"invalid": "json"}"""

        val result = handler.handleECEClick(invalidJson)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Failed to parse handle click request")
    }

    @Test
    fun `calculateShipping returns success response when parsing succeeds`() {
        val shippingRequest = ShippingCalculationRequest(
            shippingAddress = ShippingCalculationRequest.ShippingAddress(
                name = "John Doe",
                address = ECEPartialAddress(
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94105",
                    country = "US",
                )
            )
        )
        val fakeParser = FakeShippingCalculationRequestParser(returnValue = shippingRequest)
        val fakeHandlers = createFakeShopPayHandlers(shouldReturnUpdate = true)
        val handler = createDefaultBridgeHandler(
            shippingCalculationRequestJsonParser = fakeParser,
            shopPayHandlers = fakeHandlers
        )
        val message = """{"name": "John Doe", "address": {"city": "San Francisco", "country": "US"}}"""

        val result = handler.calculateShipping(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("data")

        val data = response.getJSONObject("data")
        assertThat(data.getJSONArray("lineItems").length()).isEqualTo(1)
        assertThat(data.getJSONArray("shippingRates").length()).isEqualTo(1)
        assertThat(data.getInt("totalAmount")).isEqualTo(100)
    }

    @Test
    fun `calculateShipping returns error response when parsing fails`() {
        val fakeParser = FakeShippingCalculationRequestParser(returnValue = null)
        val handler = createDefaultBridgeHandler(shippingCalculationRequestJsonParser = fakeParser)
        val message = """{"invalid": "json"}"""

        val result = handler.calculateShipping(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Failed to parse shipping rate request")
    }

    @Test
    fun `calculateShipping returns null when handler returns null`() {
        val shippingRequest = ShippingCalculationRequest(
            shippingAddress = ShippingCalculationRequest.ShippingAddress(
                name = null,
                address = ECEPartialAddress(
                    city = "San Francisco",
                    state = null,
                    postalCode = null,
                    country = "US",
                )
            )
        )
        val fakeParser = FakeShippingCalculationRequestParser(returnValue = shippingRequest)
        val fakeHandlers = createFakeShopPayHandlers(shouldReturnUpdate = false)
        val handler = createDefaultBridgeHandler(
            shippingCalculationRequestJsonParser = fakeParser,
            shopPayHandlers = fakeHandlers
        )
        val message = """{"address": {"city": "San Francisco", "country": "US"}}"""

        val result = handler.calculateShipping(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("data")
        assertThat(response.has("data")).isFalse()
    }

    @Test
    fun `calculateShippingRateChange returns success response when parsing succeeds`() {
        val shippingRateRequest = ShippingRateChangeRequest(
            shippingRate = ShopPayTestFactory.ECE_SHIPPING_RATE
        )
        val fakeParser = FakeShippingRateChangeRequestParser(returnValue = shippingRateRequest)
        val fakeHandlers = createFakeShopPayHandlers(shouldReturnUpdate = true)
        val handler = createDefaultBridgeHandler(
            shippingRateChangeRequestJsonParser = fakeParser,
            shopPayHandlers = fakeHandlers
        )
        val message = """{"shippingRate": {"id": "express", "displayName": "Express Shipping"}}"""

        val result = handler.calculateShippingRateChange(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("data")

        val data = response.getJSONObject("data")
        assertThat(data.getJSONArray("lineItems").length()).isEqualTo(1)
        assertThat(data.getJSONArray("shippingRates").length()).isEqualTo(1)
        assertThat(data.getInt("totalAmount")).isEqualTo(100)
    }

    @Test
    fun `calculateShippingRateChange returns error response when parsing fails`() {
        val fakeParser = FakeShippingRateChangeRequestParser(returnValue = null)
        val handler = createDefaultBridgeHandler(shippingRateChangeRequestJsonParser = fakeParser)
        val message = """{"invalid": "json"}"""

        val result = handler.calculateShippingRateChange(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Failed to parse shipping rate change request")
    }

    @Test
    fun `calculateShippingRateChange returns null when handler returns null`() {
        val shippingRateRequest = ShippingRateChangeRequest(
            shippingRate = ShopPayTestFactory.ECE_SHIPPING_RATE
        )
        val fakeParser = FakeShippingRateChangeRequestParser(returnValue = shippingRateRequest)
        val fakeHandlers = createFakeShopPayHandlers(shouldReturnUpdate = false)
        val handler = createDefaultBridgeHandler(
            shippingRateChangeRequestJsonParser = fakeParser,
            shopPayHandlers = fakeHandlers
        )
        val message = """{"shippingRate": {"id": "standard", "displayName": "Standard Shipping"}}"""

        val result = handler.calculateShippingRateChange(message)

        val response = JSONObject(result!!)
        assertThat(response.getString("type")).isEqualTo("data")
        assertThat(response.has("data")).isFalse()
    }

    @Test
    fun `confirmPayment returns success response when parsing succeeds`() = runTest {
        val confirmationRequest = ConfirmationRequest(
            paymentDetails = ConfirmationRequest.ConfirmEventData(
                billingDetails = ECEBillingDetails(name = "John Doe", email = null, phone = null, address = null),
                paymentMethodOptions = ECEPaymentMethodOptions(
                    shopPay = ECEPaymentMethodOptions.ShopPay(
                        externalSourceId = "src_123"
                    )
                )
            )
        )
        val fakeParser = FakeConfirmationRequestParser(returnValue = confirmationRequest)
        val handler = createDefaultBridgeHandler(confirmationRequestJsonParser = fakeParser)
        val message = """{"paymentDetails": {"billingDetails": {"name": "John Doe"}}}"""

        val result = handler.confirmPayment(message)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("data")

        val data = response.getJSONObject("data")
        assertThat(data.getString("status")).isEqualTo("success")
        assertThat(data.getBoolean("requiresAction")).isFalse()

        val confirmationState = handler.confirmationState.first()
        assertThat(confirmationState).isEqualTo(
            ShopPayConfirmationState.Success(
                externalSourceId = "src_123",
                billingDetails = confirmationRequest.paymentDetails.billingDetails
            )
        )
    }

    @Test
    fun `confirmPayment returns error response when parsing fails`() = runTest {
        val fakeParser = FakeConfirmationRequestParser(returnValue = null)
        val handler = createDefaultBridgeHandler(confirmationRequestJsonParser = fakeParser)
        val message = """{"invalid": "json"}"""

        val result = handler.confirmPayment(message)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Failed to parse confirmation request")

        val confirmationState = handler.confirmationState.first()
        assertThat(confirmationState).isInstanceOf(ShopPayConfirmationState.Failure::class.java)
        val failure = confirmationState as ShopPayConfirmationState.Failure
        assertThat(failure.cause).isInstanceOf(Exception::class.java)
        assertThat(failure.cause.message).isEqualTo("Failed to parse confirmation request")
    }

    @Test
    fun `confirmPayment returns error response when externalSourceId is missing`() = runTest {
        val confirmationRequest = ConfirmationRequest(
            paymentDetails = ConfirmationRequest.ConfirmEventData(
                billingDetails = ECEBillingDetails(name = "John Doe", email = null, phone = null, address = null),
                paymentMethodOptions = null
            )
        )
        val fakeParser = FakeConfirmationRequestParser(returnValue = confirmationRequest)
        val handler = createDefaultBridgeHandler(confirmationRequestJsonParser = fakeParser)
        val message = """{"paymentDetails": {"billingDetails": {"name": "John Doe"}}}"""

        val result = handler.confirmPayment(message)

        val response = JSONObject(result)
        assertThat(response.getString("type")).isEqualTo("error")
        assertThat(response.getString("message")).contains("Missing external source id")

        val confirmationState = handler.confirmationState.first()
        assertThat(confirmationState).isInstanceOf(ShopPayConfirmationState.Failure::class.java)
        val failure = confirmationState as ShopPayConfirmationState.Failure
        assertThat(failure.cause).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(failure.cause.message).isEqualTo("Missing external source id")
    }

    private fun createDefaultBridgeHandler(
        handleClickRequestJsonParser: ModelJsonParser<HandleClickRequest> = FakeHandleClickRequestParser(),
        shippingCalculationRequestJsonParser: ModelJsonParser<ShippingCalculationRequest> =
            FakeShippingCalculationRequestParser(),
        shippingRateChangeRequestJsonParser: ModelJsonParser<ShippingRateChangeRequest> =
            FakeShippingRateChangeRequestParser(),
        confirmationRequestJsonParser: ModelJsonParser<ConfirmationRequest> = FakeConfirmationRequestParser(),
        shopPayHandlers: ShopPayHandlers = createFakeShopPayHandlers()
    ): DefaultShopPayBridgeHandler {
        val shopPayArgs = ShopPayTestFactory.SHOP_PAY_ARGS

        return DefaultShopPayBridgeHandler(
            handleClickRequestJsonParser = handleClickRequestJsonParser,
            shopPayArgs = shopPayArgs,
            shippingRateRequestJsonParser = shippingCalculationRequestJsonParser,
            shippingRateChangeRequestJsonParser = shippingRateChangeRequestJsonParser,
            confirmationRequestJsonParser = confirmationRequestJsonParser,
            shopPayHandlers = shopPayHandlers
        )
    }

    private fun createFakeShopPayHandlers(shouldReturnUpdate: Boolean = true): ShopPayHandlers {
        return ShopPayHandlers(
            shippingMethodUpdateHandler = FakeShippingMethodHandler(shouldReturnUpdate),
            shippingContactHandler = FakeShippingContactHandler(shouldReturnUpdate)
        )
    }

    private class FakeHandleClickRequestParser(
        private val returnValue: HandleClickRequest? = null
    ) : ModelJsonParser<HandleClickRequest> {
        override fun parse(json: JSONObject): HandleClickRequest? = returnValue
    }

    private class FakeShippingCalculationRequestParser(
        private val returnValue: ShippingCalculationRequest? = null
    ) : ModelJsonParser<ShippingCalculationRequest> {
        override fun parse(json: JSONObject): ShippingCalculationRequest? = returnValue
    }

    private class FakeShippingRateChangeRequestParser(
        private val returnValue: ShippingRateChangeRequest? = null
    ) : ModelJsonParser<ShippingRateChangeRequest> {
        override fun parse(json: JSONObject): ShippingRateChangeRequest? = returnValue
    }

    private class FakeConfirmationRequestParser(
        private val returnValue: ConfirmationRequest? = null
    ) : ModelJsonParser<ConfirmationRequest> {
        override fun parse(json: JSONObject): ConfirmationRequest? = returnValue
    }

    private class FakeShippingContactHandler(
        private val shouldReturnUpdate: Boolean = true
    ) : ShopPayHandlers.ShippingContactHandler {
        override suspend fun onAddressSelected(
            address: ShopPayHandlers.SelectedAddress
        ): ShopPayHandlers.ShippingContactUpdate? {
            return if (shouldReturnUpdate) {
                ShopPayHandlers.ShippingContactUpdate(
                    lineItems = listOf(
                        PaymentSheet.ShopPayConfiguration.LineItem(
                            name = "Test Item",
                            amount = 100
                        )
                    ),
                    shippingRates = listOf(
                        PaymentSheet.ShopPayConfiguration.ShippingRate(
                            id = "test",
                            displayName = "Test Rate",
                            amount = 0,
                            deliveryEstimate = null
                        )
                    )
                )
            } else {
                null
            }
        }
    }

    private class FakeShippingMethodHandler(
        private val shouldReturnUpdate: Boolean = true
    ) : ShopPayHandlers.ShippingMethodHandler {
        override suspend fun onRateSelected(
            selectedRate: ShopPayHandlers.SelectedShippingRate
        ): ShopPayHandlers.ShippingRateUpdate? {
            return if (shouldReturnUpdate) {
                ShopPayHandlers.ShippingRateUpdate(
                    lineItems = listOf(
                        PaymentSheet.ShopPayConfiguration.LineItem(
                            name = "Test Item",
                            amount = 100
                        )
                    ),
                    shippingRates = listOf(
                        PaymentSheet.ShopPayConfiguration.ShippingRate(
                            id = "test",
                            displayName = "Test Rate",
                            amount = 0,
                            deliveryEstimate = null
                        )
                    )
                )
            } else {
                null
            }
        }
    }
}
