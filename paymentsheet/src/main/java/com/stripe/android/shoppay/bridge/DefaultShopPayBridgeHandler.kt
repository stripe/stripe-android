package com.stripe.android.shoppay.bridge

import android.webkit.JavascriptInterface
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.shoppay.ShopPayArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject

@OptIn(ShopPayPreview::class)
internal class DefaultShopPayBridgeHandler @Inject constructor(
    private val handleClickRequestJsonParser: ModelJsonParser<HandleClickRequest>,
    private val shippingRateRequestJsonParser: ModelJsonParser<ShippingCalculationRequest>,
    private val shippingRateChangeRequestJsonParser: ModelJsonParser<ShippingRateChangeRequest>,
    private val confirmationRequestJsonParser: ModelJsonParser<ConfirmationRequest>,
    private val shopPayArgs: ShopPayArgs,
    private val shopPayHandlers: ShopPayHandlers,
) : ShopPayBridgeHandler {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    private val _confirmationState = MutableStateFlow<ShopPayConfirmationState>(ShopPayConfirmationState.Pending)
    override val confirmationState: StateFlow<ShopPayConfirmationState> = _confirmationState

    @JavascriptInterface
    override fun consoleLog(level: String, message: String, origin: String, url: String) {
        val emoji = when (level.lowercase()) {
            "error" -> "❌"
            "warn" -> "⚠️"
            else -> "📝"
        }
        val frameInfo = if (origin != url) "[$origin]" else ""
        logMessage(message = "Console ${level.uppercase()}$frameInfo: $message", emoji)
    }

    @JavascriptInterface
    override fun getStripePublishableKey() = shopPayArgs.publishableKey

    @JavascriptInterface
    override fun handleECEClick(message: String) = handleRequest(message) { jsonObject ->
        val handleClickRequest = handleClickRequestJsonParser.parse(jsonObject)
            ?: throw IllegalArgumentException("Failed to parse handle click request")

        logMessage("Parsed handle click request: $handleClickRequest")

        val shopPayConfiguration = shopPayArgs.shopPayConfiguration
        HandleClickResponse(
            lineItems = shopPayConfiguration.lineItems.map { it.toECELineItem() },
            shippingRates = shopPayConfiguration.shippingRates.map { it.toECEShippingRate() },
            billingAddressRequired = shopPayConfiguration.billingAddressRequired,
            emailRequired = shopPayConfiguration.emailRequired,
            phoneNumberRequired = true, // Shop Pay always requires phone
            shippingAddressRequired = shopPayConfiguration.shippingAddressRequired,
            allowedShippingCountries = listOf("US", "CA"),
            businessName = shopPayArgs.businessName,
            shopId = shopPayConfiguration.shopId,
        )
    }

    @JavascriptInterface
    override fun getShopPayInitParams(): String {
        val amount = shopPayArgs.shopPayConfiguration.lineItems.sumOf {
            it.amount
        }
        val response = ShopPayInitParamsResponse(
            shopId = shopPayArgs.shopPayConfiguration.shopId,
            customerSessionClientSecret = shopPayArgs.customerSessionClientSecret,
            amountTotal = amount
        )
        return wrapInBridgeResponse(response)
    }

    override fun calculateShipping(message: String) = handleRequest(message) { jsonObject ->
        val calculateShippingRequest = shippingRateRequestJsonParser.parse(jsonObject)
            ?: throw IllegalArgumentException("Failed to parse shipping rate request")

        logMessage("Parsed calculateShipping request: $calculateShippingRequest")

        val partialAddress = calculateShippingRequest.shippingAddress.address
        val address = ShopPayHandlers.SelectedAddress(
            city = partialAddress.city.orEmpty(),
            state = partialAddress.state.orEmpty(),
            postalCode = partialAddress.postalCode.orEmpty(),
            country = partialAddress.country.orEmpty()
        )

        val update = shopPayHandlers.shippingContactHandler.onAddressSelected(address)
            ?: return@handleRequest null
        ShippingResponse(
            lineItems = update.lineItems.map { it.toECELineItem() },
            shippingRates = update.shippingRates.map { it.toECEShippingRate() },
            totalAmount = update.lineItems.sumOf { it.amount }
        )
    }

    @JavascriptInterface
    override fun calculateShippingRateChange(message: String) = handleRequest(message) { jsonObject ->
        val calculateShippingRateChangeRequest = shippingRateChangeRequestJsonParser.parse(jsonObject)
            ?: throw IllegalArgumentException("Failed to parse shipping rate change request")

        logMessage("Parsed calculateShippingRateChange request: $calculateShippingRateChangeRequest")

        val rate = calculateShippingRateChangeRequest.shippingRate
        val selectedShippingRate = ShopPayHandlers.SelectedShippingRate(
            shippingRate = PaymentSheet.ShopPayConfiguration.ShippingRate(
                id = rate.id,
                displayName = rate.displayName,
                amount = rate.amount,
                deliveryEstimate = null
            )
        )
        val update = shopPayHandlers.shippingMethodUpdateHandler.onRateSelected(selectedShippingRate)
            ?: return@handleRequest null
        ShippingResponse(
            lineItems = update.lineItems.map { it.toECELineItem() },
            shippingRates = update.shippingRates.map { it.toECEShippingRate() },
            totalAmount = update.lineItems.sumOf { it.amount }
        )
    }

    @JavascriptInterface
    override fun confirmPayment(message: String) = handleRequest(
        message = message,
        onError = { error ->
            _confirmationState.tryEmit(ShopPayConfirmationState.Failure(error))
        }
    ) { jsonObject ->
        val confirmationRequest = confirmationRequestJsonParser.parse(jsonObject)
            ?: throw IllegalArgumentException("Failed to parse confirmation request")

        logMessage("Parsed confirmation request: $confirmationRequest")

        val externalSourceId = confirmationRequest.paymentDetails.paymentMethodOptions?.shopPay?.externalSourceId
            ?: throw IllegalArgumentException("Missing external source id")
        _confirmationState.emit(
            value = ShopPayConfirmationState.Success(
                externalSourceId = externalSourceId,
                billingDetails = confirmationRequest.paymentDetails.billingDetails
            )
        )
        ConfirmationResponse(
            status = "success",
            requiresAction = false
        )
    }

    @JavascriptInterface
    override fun ready(message: String) {
        logMessage(message)
    }

    private fun logMessage(message: String, emoji: String = "📝") {
        logger.debug("$emoji $message")
    }

    private fun wrapInBridgeResponse(response: JsonSerializer?): String {
        return BridgeResponse.Data(data = response).toJson().toString()
    }

    private fun <T : JsonSerializer> createErrorResponse(message: String): String {
        return BridgeResponse.Error<T>(message = message).toJson().toString()
    }

    private inline fun <T : JsonSerializer> handleRequest(
        message: String,
        onError: (Throwable) -> Unit = {},
        crossinline requestHandler: suspend (JSONObject) -> T?
    ): String {
        return runCatching {
            val jsonObject = JSONObject(message)
            logMessage("Received request: $jsonObject")
            runBlocking {
                val response = requestHandler(jsonObject)
                wrapInBridgeResponse(response)
            }
        }.getOrElse { error ->
            onError(error)
            logger.error("❌ Error parsing request: ${error.message}", error)
            createErrorResponse<T>("❌ Error parsing request: ${error.message}")
        }
    }
}
