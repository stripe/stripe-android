package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.parsers.ElementsSessionJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject

/**
 * Parser for checkout session API responses:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns elements_session
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns payment_intent
 *
 * The init response contains checkout session metadata (`id`, `amount`, `currency`) and an
 * embedded `elements_session` object. The confirm response contains a `payment_intent` object.
 *
 * For confirm responses, this parser extracts the `payment_intent` and creates a minimal
 * response with the payment intent data.
 */
internal class CheckoutSessionResponseJsonParser(
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        val sessionId = json.optString(FIELD_SESSION_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = extractDueAmount(json) ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null
        val paymentIntent = json.optJSONObject(FIELD_PAYMENT_INTENT)?.let {
            PaymentIntentJsonParser().parse(it)
        }

        val elementsSession = parseElementsSession(
            json.optJSONObject(FIELD_SERVER_BUILT_ELEMENTS_SESSION_PARAMS),
            json.optJSONObject(FIELD_ELEMENTS_SESSION),
        )
        val customer = parseCustomer(json.optJSONObject(FIELD_CUSTOMER))
        val savedPaymentMethodsOfferSave = parseSavedPaymentMethodsOfferSave(
            json.optJSONObject(FIELD_SAVED_PAYMENT_METHODS_OFFER_SAVE)
        )
        val totalSummary = parseTotalSummaryResponse(json)
        val lineItems = parseLineItems(json.optJSONObject(FIELD_LINE_ITEM_GROUP))
        val shippingOptions = parseShippingOptions(json)

        return CheckoutSessionResponse(
            id = sessionId,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
            customer = customer,
            savedPaymentMethodsOfferSave = savedPaymentMethodsOfferSave,
            totalSummary = totalSummary,
            lineItems = lineItems,
            shippingOptions = shippingOptions,
        )
    }

    private fun parseElementsSessionParams(
        serverBuiltElementsSessionParams: JSONObject,
    ): ElementsSessionParams? {
        return when (serverBuiltElementsSessionParams.optString("type")) {
            "deferred_intent" -> {
                val deferredIntentJson = serverBuiltElementsSessionParams.optJSONObject("deferred_intent")
                    ?: return null
                ElementsSessionParams.DeferredIntentType(
                    locale = serverBuiltElementsSessionParams.optString("locale"),
                    deferredIntentParams = DeferredIntentParams(
                        mode = DeferredIntentParams.parseModeFromJson(deferredIntentJson)
                            ?: return null,
                        paymentMethodTypes = jsonArrayToList(
                            deferredIntentJson.optJSONArray("payment_method_types")
                        ),
                        paymentMethodConfigurationId = deferredIntentJson
                            .optString("payment_method_configuration"),
                        onBehalfOf = deferredIntentJson.optString("on_behalf_of")
                    ),
                    customPaymentMethods = jsonArrayToList(
                        serverBuiltElementsSessionParams.optJSONArray("custom_payment_methods")
                    ),
                    externalPaymentMethods = jsonArrayToList(
                        serverBuiltElementsSessionParams.optJSONArray("external_payment_methods")
                    ),
                    savedPaymentMethodSelectionId = serverBuiltElementsSessionParams
                        .optString("client_default_payment_method"),
                    mobileSessionId = serverBuiltElementsSessionParams.optString("mobile_session_id"),
                    appId = serverBuiltElementsSessionParams.optString("mobile_app_id"),
                    countryOverride = serverBuiltElementsSessionParams.optString("country_override")
                )
            }
            else -> {
                // This function is only used for parsing elements session params when init payment_pages
                // The params is always deferred intent type.
                null
            }
        }
    }

    /**
     * Parses the elements_session object if present.
     */
    private fun parseElementsSession(
        serverBuiltElementsSessionParams: JSONObject?,
        elementsSessionJson: JSONObject?,
    ): ElementsSession? {
        val serverBuiltElementsSessionParams = serverBuiltElementsSessionParams?.let {
            parseElementsSessionParams(it)
        } ?: return null
        val elementsSessionJson = elementsSessionJson ?: return null

        return ElementsSessionJsonParser(
            serverBuiltElementsSessionParams,
            isLiveMode = isLiveMode,
        ).parse(elementsSessionJson)
    }

    /**
     * Extracts amount from `total_summary.due` or `line_item_group.due` in response JSON.
     */
    private fun extractDueAmount(json: JSONObject): Long? {
        val totalSummary = json.optJSONObject(FIELD_TOTAL_SUMMARY)
        if (totalSummary != null) {
            val due = totalSummary.optLong(FIELD_DUE, -1)
            if (due >= 0) return due
        }
        val lineItemGroup = json.optJSONObject(FIELD_LINE_ITEM_GROUP)
        if (lineItemGroup != null) {
            val due = lineItemGroup.optLong(FIELD_DUE, -1)
            if (due >= 0) return due
        }
        return null
    }

    /**
     * Parses the top-level customer object from checkout session init response.
     * Customer is associated server-side when the checkout session is created,
     * so we get customer data directly in the init response.
     *
     * Expected JSON structure:
     * ```json
     * {
     *   "customer": {
     *     "id": "cus_xxx",
     *     "payment_methods": [...],
     *     "can_detach_payment_method": true
     *   }
     * }
     * ```
     */
    private fun parseCustomer(json: JSONObject?): CheckoutSessionResponse.Customer? {
        if (json == null) {
            return null
        }

        val customerId = json.optString(FIELD_CUSTOMER_ID).takeIf { it.isNotEmpty() } ?: return null
        val paymentMethodsJson = json.optJSONArray(FIELD_PAYMENT_METHODS)
        val paymentMethods = paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PaymentMethodJsonParser().parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()
        val canDetachPaymentMethod = json.optBoolean(FIELD_CAN_DETACH_PAYMENT_METHOD, false)

        return CheckoutSessionResponse.Customer(
            id = customerId,
            paymentMethods = paymentMethods,
            canDetachPaymentMethod = canDetachPaymentMethod,
        )
    }

    /**
     * Parses `customer_managed_saved_payment_methods_offer_save` from the init response.
     *
     * Expected JSON structure:
     * ```json
     * {
     *   "customer_managed_saved_payment_methods_offer_save": {
     *     "enabled": true,
     *     "status": "not_accepted"
     *   }
     * }
     * ```
     */
    private fun parseSavedPaymentMethodsOfferSave(
        json: JSONObject?,
    ): CheckoutSessionResponse.SavedPaymentMethodsOfferSave? {
        if (json == null) return null

        val enabled = json.optBoolean(FIELD_OFFER_SAVE_ENABLED, false)
        val statusString = json.optString(FIELD_OFFER_SAVE_STATUS)
        val status = when (statusString) {
            "accepted" -> CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.ACCEPTED
            else -> CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED
        }

        return CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
            enabled = enabled,
            status = status,
        )
    }

    /**
     * Parses the total summary from the response JSON.
     *
     * Reads subtotal/due/total from `total_summary` (preferred) or `line_item_group` (fallback).
     * Parses discount_amounts, tax_amounts, and shipping_rate from `line_item_group`.
     * Reads applied_balance from `total_summary`.
     */
    private fun parseTotalSummaryResponse(json: JSONObject): CheckoutSessionResponse.TotalSummaryResponse? {
        val totalSummary = json.optJSONObject(FIELD_TOTAL_SUMMARY)
        val lineItemGroup = json.optJSONObject(FIELD_LINE_ITEM_GROUP)

        // Need at least one source for amounts
        if (totalSummary == null && lineItemGroup == null) return null

        val subtotal = totalSummary?.optLong(FIELD_SUBTOTAL, -1)?.takeIf { it >= 0 }
            ?: lineItemGroup?.optLong(FIELD_SUBTOTAL, -1)?.takeIf { it >= 0 }
            ?: return null

        val totalDueToday = totalSummary?.optLong(FIELD_DUE, -1)?.takeIf { it >= 0 }
            ?: lineItemGroup?.optLong(FIELD_DUE, -1)?.takeIf { it >= 0 }
            ?: return null

        val totalAmountDue = totalSummary?.optLong(FIELD_TOTAL, -1)?.takeIf { it >= 0 }
            ?: lineItemGroup?.optLong(FIELD_TOTAL, -1)?.takeIf { it >= 0 }
            ?: return null

        val discountAmounts = parseDiscountAmounts(lineItemGroup)
        val taxAmounts = parseTaxAmounts(lineItemGroup)
        val shippingRate = parseShippingRate(lineItemGroup, json)
        val appliedBalance = totalSummary?.let {
            if (it.has(FIELD_APPLIED_BALANCE)) it.optLong(FIELD_APPLIED_BALANCE) else null
        }

        return CheckoutSessionResponse.TotalSummaryResponse(
            subtotal = subtotal,
            totalDueToday = totalDueToday,
            totalAmountDue = totalAmountDue,
            discountAmounts = discountAmounts,
            taxAmounts = taxAmounts,
            shippingRate = shippingRate,
            appliedBalance = appliedBalance,
        )
    }

    private fun parseDiscountAmounts(
        lineItemGroup: JSONObject?,
    ): List<CheckoutSessionResponse.DiscountAmount> {
        val array = lineItemGroup?.optJSONArray(FIELD_DISCOUNT_AMOUNTS) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val amount = obj.optLong(FIELD_AMOUNT, -1).takeIf { it >= 0 } ?: return@mapNotNull null
            val displayName = obj.optJSONObject(FIELD_COUPON)?.optString(FIELD_NAME)
                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            CheckoutSessionResponse.DiscountAmount(amount = amount, displayName = displayName)
        }
    }

    private fun parseTaxAmounts(
        lineItemGroup: JSONObject?,
    ): List<CheckoutSessionResponse.TaxAmount> {
        val array = lineItemGroup?.optJSONArray(FIELD_TAX_AMOUNTS) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val amount = obj.optLong(FIELD_AMOUNT, -1).takeIf { it > 0 } ?: return@mapNotNull null
            val inclusive = obj.optBoolean(FIELD_INCLUSIVE, false)
            val taxRate = obj.optJSONObject(FIELD_TAX_RATE) ?: return@mapNotNull null
            val displayName = taxRate.optString(FIELD_DISPLAY_NAME).takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val percentage = taxRate.optDouble(FIELD_PERCENTAGE, Double.NaN)
            if (percentage.isNaN()) return@mapNotNull null
            CheckoutSessionResponse.TaxAmount(
                amount = amount,
                inclusive = inclusive,
                displayName = displayName,
                percentage = percentage,
            )
        }
    }

    private fun parseShippingRate(
        lineItemGroup: JSONObject?,
        rootJson: JSONObject,
    ): CheckoutSessionResponse.ShippingRate? {
        // Primary: line_item_group.shipping_rate
        val shippingRateJson = lineItemGroup?.optJSONObject(FIELD_SHIPPING_RATE)
        if (shippingRateJson != null) {
            return parseShippingRateFromJson(shippingRateJson)
        }
        // Fallback: shipping.shipping_option
        val shippingOption = rootJson.optJSONObject(FIELD_SHIPPING)
            ?.optJSONObject(FIELD_SHIPPING_OPTION)
        if (shippingOption != null) {
            return parseShippingRateFromJson(shippingOption)
        }
        return null
    }

    private fun parseShippingRateFromJson(json: JSONObject): CheckoutSessionResponse.ShippingRate? {
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = json.optLong(FIELD_AMOUNT, -1).takeIf { it >= 0 } ?: return null
        val displayName = json.optString(FIELD_DISPLAY_NAME).takeIf { it.isNotEmpty() } ?: return null
        val deliveryEstimate = parseDeliveryEstimate(json)
        return CheckoutSessionResponse.ShippingRate(
            id = id,
            amount = amount,
            displayName = displayName,
            deliveryEstimate = deliveryEstimate,
        )
    }

    private fun parseShippingOptions(json: JSONObject): List<CheckoutSessionResponse.ShippingRate> {
        val array = json.optJSONArray(FIELD_SHIPPING_OPTIONS) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val shippingRateJson = obj.optJSONObject(FIELD_SHIPPING_RATE) ?: return@mapNotNull null
            parseShippingRateFromJson(shippingRateJson)
        }
    }

    private fun parseDeliveryEstimate(json: JSONObject): String? {
        if (!json.has(FIELD_DELIVERY_ESTIMATE)) return null
        // If it's a string, use directly
        val stringValue = json.optString(FIELD_DELIVERY_ESTIMATE).takeIf { it.isNotEmpty() }
        if (stringValue != null && !json.optJSONObject(FIELD_DELIVERY_ESTIMATE).let { it != null }) {
            return stringValue
        }
        // If it's an object with minimum/maximum, format as "N-M business days"
        val estimateObj = json.optJSONObject(FIELD_DELIVERY_ESTIMATE) ?: return null
        val minimum = estimateObj.optJSONObject("minimum")
        val maximum = estimateObj.optJSONObject("maximum")
        val minValue = minimum?.optInt("value", -1)?.takeIf { it >= 0 }
        val maxValue = maximum?.optInt("value", -1)?.takeIf { it >= 0 }
        val unit = minimum?.optString("unit")
            ?: maximum?.optString("unit")
            ?: "business_day"
        val unitDisplay = unit.replace("_", " ") + "s"
        return when {
            minValue != null && maxValue != null -> "$minValue-$maxValue $unitDisplay"
            minValue != null -> "$minValue+ $unitDisplay"
            maxValue != null -> "Up to $maxValue $unitDisplay"
            else -> null
        }
    }

    private fun parseLineItems(
        lineItemGroup: JSONObject?,
    ): List<CheckoutSessionResponse.LineItem> {
        val array = lineItemGroup?.optJSONArray(FIELD_LINE_ITEMS) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val name = obj.optString(FIELD_NAME).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val quantity = obj.optInt(FIELD_QUANTITY, -1).takeIf { it > 0 } ?: return@mapNotNull null
            val subtotal = obj.optLong(FIELD_SUBTOTAL, -1).takeIf { it >= 0 } ?: return@mapNotNull null
            val total = obj.optLong(FIELD_TOTAL, -1).takeIf { it >= 0 } ?: return@mapNotNull null
            val unitAmount = if (quantity > 0) total / quantity else null
            CheckoutSessionResponse.LineItem(
                id = id,
                name = name,
                quantity = quantity,
                unitAmount = unitAmount,
                subtotal = subtotal,
                total = total,
            )
        }
    }

    private companion object {
        private const val FIELD_SESSION_ID = "session_id"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_ELEMENTS_SESSION = "elements_session"
        private const val FIELD_TOTAL_SUMMARY = "total_summary"
        private const val FIELD_DUE = "due"
        private const val FIELD_PAYMENT_INTENT = "payment_intent"
        private const val FIELD_SERVER_BUILT_ELEMENTS_SESSION_PARAMS = "server_built_elements_session_params"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_CUSTOMER_ID = "id"
        private const val FIELD_PAYMENT_METHODS = "payment_methods"
        private const val FIELD_CAN_DETACH_PAYMENT_METHOD = "can_detach_payment_method"
        private const val FIELD_SAVED_PAYMENT_METHODS_OFFER_SAVE =
            "customer_managed_saved_payment_methods_offer_save"
        private const val FIELD_OFFER_SAVE_ENABLED = "enabled"
        private const val FIELD_OFFER_SAVE_STATUS = "status"
        private const val FIELD_LINE_ITEM_GROUP = "line_item_group"
        private const val FIELD_SUBTOTAL = "subtotal"
        private const val FIELD_TOTAL = "total"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_APPLIED_BALANCE = "applied_balance"
        private const val FIELD_DISCOUNT_AMOUNTS = "discount_amounts"
        private const val FIELD_COUPON = "coupon"
        private const val FIELD_NAME = "name"
        private const val FIELD_TAX_AMOUNTS = "tax_amounts"
        private const val FIELD_INCLUSIVE = "inclusive"
        private const val FIELD_TAX_RATE = "tax_rate"
        private const val FIELD_DISPLAY_NAME = "display_name"
        private const val FIELD_PERCENTAGE = "percentage"
        private const val FIELD_SHIPPING_RATE = "shipping_rate"
        private const val FIELD_SHIPPING = "shipping"
        private const val FIELD_SHIPPING_OPTION = "shipping_option"
        private const val FIELD_SHIPPING_OPTIONS = "shipping_options"
        private const val FIELD_DELIVERY_ESTIMATE = "delivery_estimate"
        private const val FIELD_LINE_ITEMS = "line_items"
        private const val FIELD_ID = "id"
        private const val FIELD_QUANTITY = "quantity"
    }
}
