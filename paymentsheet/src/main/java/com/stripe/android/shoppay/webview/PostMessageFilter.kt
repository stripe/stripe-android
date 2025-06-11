package com.stripe.android.shoppay.webview

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder

interface PostMessageFilter {
    fun filter(message: String): StripeParentEvent? // Updated return type
}

class DefaultPostMessageFilter : PostMessageFilter {
    private val paymentMapper = PaymentConfirmationDataMapper()

    override fun filter(message: String): StripeParentEvent? {
        val initialJsonObject: JSONObject
        try {
            initialJsonObject = JSONObject(message)
        } catch (e: JSONException) {
            Log.e("PostMessageFilter", "Initial message is not valid JSON: $message", e)
            return null
        }

        val type = initialJsonObject.optString("type")
        if (type != "messageEvent") return null

        // Check if this is a ping message (data is direct JSON object)
        val dataObject = initialJsonObject.optJSONObject("data")
        if (dataObject != null && dataObject.optString("messageType") == "ping") {
            return handlePingMessage(initialJsonObject, dataObject)
        }

        // Handle existing message format (data is string containing JSON)
        val dataString = initialJsonObject.optString("data", null) ?: return null
        val dataJsonObject: JSONObject
        try {
            dataJsonObject = JSONObject(dataString)
        } catch (e: JSONException) {
            // Log.e("Filter", "data field is not valid JSON string: $dataString", e)
            return null
        }

        val dataType = dataJsonObject.optString("type")
        if (dataType != "parent") return null

        // Extract common identifiers
        val sourceFrameId = dataJsonObject.optString("sourceFrameId", null)
        val controllerAppFrameId = dataJsonObject.optString("controllerAppFrameId", null)

        val messageObj = dataJsonObject.optJSONObject("message") ?: return null
        val action = messageObj.optString("action")

        // Most relevant events are under "stripe-frame-event"
        if (action != "stripe-frame-event") {
            // Optionally handle other actions like "stripe-controller-action-response" if needed
            // For now, return a generic event or null if these are not of interest
            return GenericStripeParentEvent(
                eventType = "action:$action", // Prefix to distinguish
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                payloadData = messageObj.optJSONObject("payload")
            )
        }

        val payloadObj = messageObj.optJSONObject("payload") ?: return null
        val eventType = payloadObj.optString("event", null) ?: return null
        val eventSpecificData = payloadObj.optJSONObject("data")

        return when (eventType) {
            "confirm" -> ConfirmEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                paymentDetails = paymentMapper.map(eventSpecificData)
            )
            "element-loader-ui-callback" -> ElementLoaderUiCallbackEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                componentName = eventSpecificData?.optJSONObject("message")?.optString("componentName", null),
                loaderUiNodes = eventSpecificData?.optJSONObject("message")?.optJSONObject("loaderUiNodes")
            )
            "change" -> {
                val valueJson = eventSpecificData?.optJSONObject("value")
                val availablePaymentMethods = eventSpecificData?.optJSONObject("availablePaymentMethods")
                ElementChangeEvent(
                    eventType = eventType,
                    sourceFrameId = sourceFrameId,
                    controllerAppFrameId = controllerAppFrameId,
                    value = valueJson,
                    empty = eventSpecificData?.optBoolean("empty"),
                    complete = eventSpecificData?.optBoolean("complete"),
                    elementMode = eventSpecificData?.optString("elementMode", null),
                    collapsed = eventSpecificData?.optBoolean("collapsed"),
                    availablePaymentMethods = if((availablePaymentMethods?.length() ?: 0) > 0) availablePaymentMethods else null
                )
            }
            "title" -> FrameTitleEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                title = eventSpecificData?.optString("title", null)
            )
            "ready" -> ElementReadyEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                data = eventSpecificData // Can be empty {} or have content
            )
            "click" -> ElementClickEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                nonce = eventSpecificData?.optString("nonce", null),
                paymentMethodType = eventSpecificData?.optString("paymentMethodType", null)
                // other click data can be extracted from eventSpecificData if needed
            )
            "set_styles" -> SetStylesEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                styles = eventSpecificData
            )
            "load" -> LoadEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                data = eventSpecificData // Usually empty
            )
            "update-apple-pay", "update-google-pay" -> {
                val networks = mutableListOf<String>()
                eventSpecificData?.optJSONArray("capabilityEnabledCardNetworks")?.let {
                    for(i in 0 until it.length()) networks.add(it.getString(i))
                }
                UpdatePaymentMethodEvent(
                    eventType = eventType,
                    sourceFrameId = sourceFrameId,
                    controllerAppFrameId = controllerAppFrameId,
                    merchantDetails = eventSpecificData?.optJSONObject("__merchantDetails"),
                    country = eventSpecificData?.optString("country", null),
                    currency = eventSpecificData?.optString("currency", null),
                    total = eventSpecificData?.optJSONObject("total"),
                    blockedCardBrands = eventSpecificData?.optJSONArray("blockedCardBrands")?.let { arr -> List(arr.length()) { arr.getString(it) } },
                    capabilityEnabledCardNetworks = networks.ifEmpty { null }
                )
            }
            "setup-us-bank-account", "setup-stripe-google-maps-autocomplete" -> SetupEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                data = eventSpecificData // Usually empty
            )
            "dismiss-overlay" -> DismissOverlayEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                data = eventSpecificData
            )
            else -> GenericStripeParentEvent(
                eventType = eventType,
                sourceFrameId = sourceFrameId,
                controllerAppFrameId = controllerAppFrameId,
                payloadData = eventSpecificData
            )
        }
    }

    private fun handlePingMessage(initialJsonObject: JSONObject, dataObject: JSONObject): PingEvent {
        val origin = initialJsonObject.optString("origin", null)
        val source = initialJsonObject.optString("source", null)
        val timestamp = initialJsonObject.optLong("timestamp", 0L).takeIf { it != 0L }
        val currentFrame = initialJsonObject.optString("currentFrame", null)
        val messageType = dataObject.optString("messageType", null)
        
        val checkoutState = currentFrame?.let { parseCheckoutStateFromUrl(it) }
        
        return PingEvent(
            eventType = "ping",
            sourceFrameId = null, // Ping messages don't have frame IDs
            controllerAppFrameId = null,
            messageType = messageType,
            origin = origin,
            source = source,
            timestamp = timestamp,
            currentFrame = currentFrame,
            checkoutState = checkoutState
        )
    }
    
    private fun parseCheckoutStateFromUrl(url: String): CheckoutState? {
        return try {
            // Extract checkoutState parameter from URL
            val checkoutStateParam = url.substringAfter("checkoutState=", "")
                .substringBefore("&")
                .takeIf { it.isNotEmpty() } ?: return null
            
            // URL decode the parameter
            val decodedParam = URLDecoder.decode(checkoutStateParam, "UTF-8")
            
            // Log for debugging
            Log.d("PostMessageFilter", "Parsing checkout state: $decodedParam")
            
            // Parse the JSON
            val checkoutJson = JSONObject(decodedParam)
            
            // Extract currency and amount
            val currency = checkoutJson.optString("currency", null)
            val amount = checkoutJson.optInt("amount", -1).takeIf { it != -1 }
            
            // Extract line items
            val lineItemsArray = checkoutJson.optJSONArray("lineItems")
            val lineItems = mutableListOf<CheckoutState.LineItem>()
            lineItemsArray?.let { array ->
                for (i in 0 until array.length()) {
                    val itemJson = array.optJSONObject(i)
                    if (itemJson != null) {
                        val name = itemJson.optString("name", null)
                        val itemAmount = itemJson.optInt("amount", -1).takeIf { it != -1 }
                        lineItems.add(CheckoutState.LineItem(name, itemAmount))
                    }
                }
            }
            
            // Extract shipping rates (currently empty array in the example)
            val shippingRatesArray = checkoutJson.optJSONArray("shippingRates")
            val shippingRates = mutableListOf<String>()
            shippingRatesArray?.let { array ->
                for (i in 0 until array.length()) {
                    array.optString(i)?.let { rate ->
                        shippingRates.add(rate)
                    }
                }
            }
            
            val result = CheckoutState(
                currency = currency,
                amount = amount,
                lineItems = lineItems.ifEmpty { null },
                shippingRates = shippingRates.ifEmpty { null }
            )
            
            Log.d("PostMessageFilter", "Successfully parsed checkout state: $result")
            result
        } catch (e: Exception) {
            Log.e("PostMessageFilter", "Failed to parse checkout state from URL: $url", e)
            null
        }
    }
}

// Your PaymentConfirmationDataMapper (ensure BillingDetails mapping is complete)
class PaymentConfirmationDataMapper {
    private fun mapAddress(addressJson: JSONObject?): PaymentConfirmationData.Address? {
        return addressJson?.let { addr ->
            PaymentConfirmationData.Address(
                line1 = addr.optString("line1", null),
                line2 = addr.optString("line2", null),
                city = addr.optString("city", null),
                state = addr.optString("state", null),
                postalCode = addr.optString("postal_code", null)
            )
        }
    }

    fun map(jsonObject: JSONObject?): PaymentConfirmationData? {
        if (jsonObject == null) return null

        val paymentMethodType = jsonObject.optString("paymentMethodType", null)
        val nonce = jsonObject.optString("nonce", null)

        val shippingAddressJson = jsonObject.optJSONObject("shippingAddress")
        val shippingAddress = shippingAddressJson?.let {
            val name = it.optString("name", null)
            val address = mapAddress(it.optJSONObject("address"))
            PaymentConfirmationData.ShippingAddress(name, address)
        }

        val billingDetailsJson = jsonObject.optJSONObject("billingDetails")
        val billingDetails = billingDetailsJson?.let { bdJson ->
            val name = bdJson.optString("name", null)
            val email = bdJson.optString("email", null) // Assuming email and phone might be present
            val phone = bdJson.optString("phone", null)
            val address = mapAddress(bdJson.optJSONObject("address"))
            PaymentConfirmationData.BillingDetails(name, email, phone, address)
        }

        return PaymentConfirmationData(paymentMethodType, nonce, shippingAddress, billingDetails)
    }
}