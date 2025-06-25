package com.stripe.android.shoppay.bridge

import android.webkit.JavascriptInterface
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.shoppay.ShopPayArgs
import org.json.JSONObject
import javax.inject.Inject

internal class DefaultBridgeHandler @Inject constructor(
    private val handleClickRequestJsonParser: ModelJsonParser<HandleClickRequest>,
    private val shopPayArgs: ShopPayArgs
) : BridgeHandler {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    @JavascriptInterface
    override fun consoleLog(level: String, message: String, origin: String, url: String) {
        val emoji = when (level.lowercase()) {
            "error" -> "❌"
            "warn" -> "⚠️"
            else -> "📝"
        }
        val frameInfo = if (origin != url) "[$origin]" else ""
        logMessage(emoji, "Console ${level.uppercase()}$frameInfo: $message")
    }

    @JavascriptInterface
    override fun getStripePublishableKey() = shopPayArgs.publishableKey

    @JavascriptInterface
    override fun handleECEClick(message: String): String {
        return runCatching {
            val jsonObject = JSONObject(message)
            val handleClickRequest = handleClickRequestJsonParser.parse(jsonObject)
                ?: return@runCatching createErrorResponse<HandleClickResponse>("Failed to parse handle click request")

            logMessage(
                message = "Parsed handle click request: expressPaymentType=" +
                    handleClickRequest.eventData.expressPaymentType
            )

            val shopPayConfiguration = shopPayArgs.shopPayConfiguration
            val response = HandleClickResponse(
                lineItems = shopPayConfiguration.lineItems,
                shippingRates = shopPayConfiguration.shippingRates,
                billingAddressRequired = shopPayConfiguration.billingAddressRequired,
                emailRequired = shopPayConfiguration.emailRequired,
                phoneNumberRequired = true, // Shop Pay always requires phone
                shippingAddressRequired = shopPayConfiguration.shippingAddressRequired,
                allowedShippingCountries = listOf("US", "CA"),
                businessName = shopPayArgs.businessName,
                shopId = shopPayConfiguration.shopId,
            )
            wrapInBridgeResponse(response)
        }.getOrElse {
            logger.error("❌ Error parsing handle click request", it)
            createErrorResponse<HandleClickResponse>("❌ Error parsing handle click request: ${it.message}")
        }
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

    @JavascriptInterface
    override fun ready(message: String) {
        logMessage(message = message)
    }

    private fun logMessage(emoji: String = "📝", message: String) {
        logger.debug("$emoji $message")
    }

    private fun wrapInBridgeResponse(response: JsonSerializer): String {
        return BridgeResponse.Data(data = response).toJson().toString()
    }

    private fun <T : JsonSerializer> createErrorResponse(message: String): String {
        return BridgeResponse.Error<T>(message = message).toJson().toString()
    }
}
