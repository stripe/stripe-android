package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.ElementsSessionJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import org.json.JSONArray
import org.json.JSONObject

/**
 * Strict parser for the modeless, unified Payment Pages response.
 */
internal object CheckoutSessionResponseJsonParser : ModelJsonParser<CheckoutSessionResponse> {
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun parse(json: JSONObject): CheckoutSessionResponse? = runCatching {
        require(json.requiredString("ui_mode") == "custom")
        require(json.requiredString("mode") == "modeless")
        val id = json.requiredString("session_id")
        val currency = json.requiredString("currency")
        val paymentStatus = when (json.requiredString("payment_status")) {
            "paid" -> CheckoutSessionResponse.PaymentStatus.PAID
            "unpaid" -> CheckoutSessionResponse.PaymentStatus.UNPAID
            "no_payment_required" -> CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED
            else -> error("Unsupported payment_status")
        }
        val status = when (json.requiredString("status")) {
            "open" -> CheckoutSessionResponse.Status.OPEN
            "complete" -> CheckoutSessionResponse.Status.COMPLETE
            "expired" -> CheckoutSessionResponse.Status.EXPIRED
            else -> error("Unsupported status")
        }

        val checkoutItems = json.requiredArray("checkout_items").objects().map(::parseCheckoutItem)
        require(checkoutItems.isNotEmpty())
        require(
            checkoutItems.all { group ->
                group.oneTimePrice.items.all { it.price.currency == currency }
            }
        )

        val livemode = json.requiredBoolean("livemode")
        val elementsJson = json.requiredObject("elements_session")
        val elementsSession = parseElementsSession(
            serverBuiltElementsSessionParams = json.requiredObject("server_built_elements_session_params"),
            elementsSessionJson = elementsJson,
            livemode = livemode,
            noPaymentRequired = paymentStatus == CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED,
        ) ?: error("Invalid Elements Session")
        val merchantCountry = elementsSession.merchantCountry ?: error("Missing merchant country")
        val taxContext = json.optionalObject("tax_context")

        val paymentIntent = json.optionalObject("payment_intent")?.let { PaymentIntentJsonParser().parse(it) }
        val setupIntent = json.optionalObject("setup_intent")?.let { SetupIntentJsonParser().parse(it) }
        val confirmedIntent = paymentIntent ?: setupIntent

        CheckoutSessionResponse(
            id = id,
            currency = currency,
            paymentStatus = paymentStatus,
            status = status,
            livemode = livemode,
            customerEmail = StripeJsonUtils.optString(json, "customer_email"),
            elementsSession = if (confirmedIntent == null) {
                elementsSession
            } else {
                elementsSession.copy(stripeIntent = confirmedIntent)
            },
            paymentIntent = paymentIntent,
            setupIntent = setupIntent,
            customer = json.optionalObject("customer")?.let(::parseCustomer),
            savedPaymentMethodsOfferSave = json.optionalObject(
                "customer_managed_saved_payment_methods_offer_save"
            )?.let(::parseSavedPaymentMethodsOfferSave),
            checkoutItems = checkoutItems,
            recurringDetails = json.optionalObject("recurring_details")?.let(::parseRecurringDetails),
            adaptivePricingInfo = json.optionalObject("adaptive_pricing_info")?.let(::parseAdaptivePricingInfo),
            taxMeta = json.optionalObject("tax_meta")?.let(::parseTaxMeta),
            automaticTaxEnabled = taxContext?.optionalBoolean("automatic_tax_enabled") ?: false,
            taxAddressSource = parseTaxAddressSource(taxContext),
            allowedShippingCountries = json.optionalObject("shipping_address_collection")
                ?.requiredArray("allowed_countries")?.strings(),
            requiresShippingAddress = json.has("shipping_address_collection"),
            requiresBillingAddress = json.optString("billing_address_collection") == "required",
            merchantCountry = merchantCountry,
            businessName = StripeJsonUtils.optString(elementsJson, "business_name"),
        )
    }.getOrNull()

    private fun parseCheckoutItem(json: JSONObject): CheckoutSessionResponse.CheckoutItem {
        require(json.requiredString("type") == "one_time_price")
        return CheckoutSessionResponse.CheckoutItem(
            key = json.requiredString("key"),
            oneTimePrice = CheckoutSessionResponse.OneTimePrice(
                items = json.requiredObject("one_time_price").requiredArray("items").objects().map(::parsePriceItem)
            ),
        ).also { require(it.oneTimePrice.items.isNotEmpty()) }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun parsePriceItem(json: JSONObject): CheckoutSessionResponse.OneTimePriceItem {
        val priceJson = json.requiredObject("price")
        val price = CheckoutSessionResponse.Price(
            id = priceJson.requiredString("id"),
            currency = priceJson.requiredString("currency"),
            unitAmount = priceJson.optionalLong("unit_amount")?.also { require(it >= 0) },
            product = priceJson.requiredObject("product").let { product ->
                CheckoutSessionResponse.Product(
                    name = product.requiredString("name"),
                    images = product.requiredArray("images").strings(),
                )
            },
        )
        val unitAmount = json.optionalLong("unit_amount")?.also { require(it >= 0) }
        val unitAmountDecimal = json.optionalString("unit_amount_decimal")?.toDoubleOrNull()?.also {
            require(it.isFinite() && it >= 0)
        }
        require(unitAmount != null || price.unitAmount != null || unitAmountDecimal != null)
        val adjustableQuantity = json.optionalObject("adjustable_quantity")?.let { adjustable ->
            val enabled = adjustable.requiredBoolean("enabled")
            val maximum = adjustable.optionalInt("maximum")
            val minimum = adjustable.optionalInt("minimum")
            if (enabled) require(maximum != null && minimum != null)
            CheckoutSessionResponse.AdjustableQuantity(enabled, maximum, minimum)
        }
        return CheckoutSessionResponse.OneTimePriceItem(
            innerItemKey = json.requiredString("inner_item_key"),
            price = price,
            quantity = json.requiredInt("quantity").also { require(it >= 0) },
            subtotal = json.requiredLong("subtotal").also { require(it >= 0) },
            total = json.requiredLong("total").also { require(it >= 0) },
            unitAmount = unitAmount,
            unitAmountDecimal = unitAmountDecimal,
            unitLabel = json.optionalString("unit_label"),
            taxAmounts = json.requiredArray("tax_amounts").objects().map(::parseTaxAmount),
            taxInclusive = json.requiredLong("tax_inclusive").also { require(it >= 0) },
            taxExclusive = json.requiredLong("tax_exclusive").also { require(it >= 0) },
            adjustableQuantity = adjustableQuantity,
        )
    }

    private fun parseRecurringDetails(json: JSONObject) = CheckoutSessionResponse.RecurringDetails(
        totalDiscountAmounts = json.requiredArray("total_discount_amounts").objects().map(::parseDiscountAmount),
        totalTaxAmounts = json.requiredArray("total_tax_amounts").objects().map(::parseTaxAmount),
    )

    private fun parseDiscountAmount(json: JSONObject): CheckoutSessionResponse.DiscountAmount {
        val coupon = json.requiredObject("coupon")
        return CheckoutSessionResponse.DiscountAmount(
            amount = json.requiredLong("amount").also { require(it >= 0) },
            displayName = json.optionalString("display_name"),
            coupon = CheckoutSessionResponse.Coupon(
                code = coupon.requiredString("code"),
                name = coupon.optionalString("name"),
                percentOff = coupon.optionalFiniteDouble("percent_off"),
            ),
            promotionCode = json.optionalObject("promotion_code")?.let {
                CheckoutSessionResponse.PromotionCode(it.optionalString("code"))
            },
        )
    }

    private fun parseTaxAmount(json: JSONObject): CheckoutSessionResponse.TaxAmount {
        val taxRate = json.requiredObject("tax_rate")
        val rateType = when (val value = taxRate.optionalString("rate_type")) {
            null -> null
            "flat_amount" -> CheckoutSessionResponse.TaxRateType.FLAT_AMOUNT
            "percentage" -> CheckoutSessionResponse.TaxRateType.PERCENTAGE
            else -> error("Unsupported tax rate type: $value")
        }
        return CheckoutSessionResponse.TaxAmount(
            amount = json.requiredLong("amount").also { require(it >= 0) },
            inclusive = json.requiredBoolean("inclusive"),
            taxRate = CheckoutSessionResponse.TaxRate(
                displayName = taxRate.requiredString("display_name"),
                percentage = taxRate.requiredFiniteDouble("percentage"),
                rateType = rateType,
            ),
        )
    }

    private fun parseTaxMeta(json: JSONObject): CheckoutSessionResponse.TaxMeta {
        val computationType = when (json.requiredString("computation_type")) {
            "Off" -> CheckoutSessionResponse.TaxComputationType.OFF
            "automatic" -> CheckoutSessionResponse.TaxComputationType.AUTOMATIC
            "extension_defined" -> CheckoutSessionResponse.TaxComputationType.EXTENSION_DEFINED
            "manual" -> CheckoutSessionResponse.TaxComputationType.MANUAL
            "user_defined" -> CheckoutSessionResponse.TaxComputationType.USER_DEFINED
            else -> error("Unsupported tax computation type")
        }
        val status = when (val value = json.optionalString("status")) {
            null -> null
            "complete" -> CheckoutSessionResponse.TaxStatus.COMPLETE
            "failed" -> CheckoutSessionResponse.TaxStatus.FAILED
            "requires_location_inputs" -> CheckoutSessionResponse.TaxStatus.REQUIRES_LOCATION_INPUTS
            else -> error("Unsupported tax status: $value")
        }
        return CheckoutSessionResponse.TaxMeta(computationType, status)
    }

    private fun parseAdaptivePricingInfo(json: JSONObject) = CheckoutSessionResponse.AdaptivePricingInfo(
        activePresentmentCurrency = json.requiredString("active_presentment_currency"),
        integrationAmount = json.requiredLong("integration_amount").also { require(it >= 0) },
        integrationCurrency = json.requiredString("integration_currency"),
        localCurrencyOptions = json.requiredArray("local_currency_options").objects().map { option ->
            CheckoutSessionResponse.LocalCurrencyOption(
                amount = option.requiredLong("amount").also { require(it >= 0) },
                conversionMarkupBps = option.optionalInt("conversion_markup_bps"),
                currency = option.requiredString("currency"),
                presentmentExchangeRate = option.requiredString("presentment_exchange_rate"),
            )
        },
    )

    private fun parseTaxAddressSource(json: JSONObject?): CheckoutSessionResponse.TaxAddressSource? {
        val raw = json?.optionalString("automatic_tax_address_source")?.removePrefix("session.")
        return when (raw) {
            "shipping" -> CheckoutSessionResponse.TaxAddressSource.SHIPPING
            "billing" -> CheckoutSessionResponse.TaxAddressSource.BILLING
            else -> null
        }
    }

    private fun parseCustomer(json: JSONObject): CheckoutSessionResponse.Customer {
        return CheckoutSessionResponse.Customer(
            id = json.requiredString("id"),
            paymentMethods = json.requiredArray("payment_methods").objects().map { paymentMethod ->
                PaymentMethodJsonParser().parse(paymentMethod) ?: error("Invalid payment method")
            },
            canDetachPaymentMethod = json.optionalBoolean("can_detach_payment_method") ?: false,
        )
    }

    private fun parseSavedPaymentMethodsOfferSave(json: JSONObject) =
        CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
            enabled = json.requiredBoolean("enabled"),
            status = when (json.requiredString("status")) {
                "accepted" -> CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.ACCEPTED
                "not_accepted" -> CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED
                else -> error("Unsupported saved-payment-method status")
            },
        )

    private fun parseElementsSession(
        serverBuiltElementsSessionParams: JSONObject,
        elementsSessionJson: JSONObject,
        livemode: Boolean,
        noPaymentRequired: Boolean,
    ): ElementsSession? {
        if (serverBuiltElementsSessionParams.optString("type") != "deferred_intent") return null
        val deferred = serverBuiltElementsSessionParams.optJSONObject("deferred_intent") ?: return null
        val params = ElementsSessionParams.DeferredIntentType(
            locale = serverBuiltElementsSessionParams.optString("locale"),
            deferredIntentParams = DeferredIntentParams(
                mode = if (noPaymentRequired) {
                    DeferredIntentParams.Mode.Setup(
                        currency = deferred.optString("currency").takeIf { it.isNotEmpty() },
                        setupFutureUsage = deferred.optString("setup_future_usage").let { code ->
                            StripeIntent.Usage.entries.firstOrNull { it.code == code }
                        } ?: StripeIntent.Usage.OffSession,
                    )
                } else {
                    DeferredIntentParams.parseModeFromJson(deferred) ?: return null
                },
                paymentMethodTypes = jsonArrayToList(deferred.optJSONArray("payment_method_types")),
                paymentMethodConfigurationId = deferred.optString("payment_method_configuration"),
                onBehalfOf = deferred.optString("on_behalf_of"),
            ),
            customPaymentMethods = jsonArrayToList(
                serverBuiltElementsSessionParams.optJSONArray("custom_payment_methods")
            ),
            externalPaymentMethods = jsonArrayToList(
                serverBuiltElementsSessionParams.optJSONArray("external_payment_methods")
            ),
            savedPaymentMethodSelectionId = serverBuiltElementsSessionParams.optString("client_default_payment_method"),
            mobileSessionId = serverBuiltElementsSessionParams.optString("mobile_session_id"),
            appId = serverBuiltElementsSessionParams.optString("mobile_app_id"),
            countryOverride = serverBuiltElementsSessionParams.optString("country_override"),
        )
        return ElementsSessionJsonParser(params, isLiveMode = livemode).parse(elementsSessionJson)
    }

    private fun JSONObject.requiredString(name: String): String = getString(name).also { require(it.isNotEmpty()) }
    private fun JSONObject.optionalString(name: String): String? =
        if (!has(name) || isNull(name)) null else getString(name)
    private fun JSONObject.requiredObject(name: String): JSONObject = getJSONObject(name)
    private fun JSONObject.optionalObject(name: String): JSONObject? =
        if (!has(name) || isNull(name)) null else getJSONObject(name)
    private fun JSONObject.requiredArray(name: String): JSONArray = getJSONArray(name)
    private fun JSONObject.requiredBoolean(name: String): Boolean = getBoolean(name)
    private fun JSONObject.optionalBoolean(name: String): Boolean? =
        if (!has(name) || isNull(name)) null else getBoolean(name)
    private fun JSONObject.requiredLong(name: String): Long = getLong(name)
    private fun JSONObject.optionalLong(name: String): Long? =
        if (!has(name) || isNull(name)) null else getLong(name)
    private fun JSONObject.requiredInt(name: String): Int = getInt(name)
    private fun JSONObject.optionalInt(name: String): Int? =
        if (!has(name) || isNull(name)) null else getInt(name)
    private fun JSONObject.requiredFiniteDouble(name: String): Double = getDouble(name).also { require(it.isFinite()) }
    private fun JSONObject.optionalFiniteDouble(name: String): Double? =
        if (!has(name) || isNull(name)) null else requiredFiniteDouble(name)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
}
