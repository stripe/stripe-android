package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.DeferredPaymentIntentJsonParser
import com.stripe.android.model.parsers.DeferredSetupIntentJsonParser
import com.stripe.android.model.parsers.ElementsSessionJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses CheckoutClient responses without depending on the legacy Elements Session wire format.
 */
internal class CheckoutSessionResponseJsonParser(
    private val clientSecret: String,
) : ModelJsonParser<CheckoutSessionResponse> {
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun parse(json: JSONObject): CheckoutSessionResponse? = runCatching {
        require(json.requiredString("ui_mode") == "mobile_elements")
        val id = json.requiredString("id")
        val configId = json.optionalString("checkout_session_config_id")
        val automaticPaymentMethodTypes = json.optionalBoolean("automatic_payment_method_types")
        val currency = json.requiredString("presentment_currency")
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

        val checkoutItems = json.requiredArray("items").objects().map(::parseCheckoutItem)
        require(checkoutItems.isNotEmpty())
        require(
            checkoutItems.all { group ->
                group.oneTimePrice.items.all { it.price.currency == currency }
            }
        )

        val livemode = json.requiredBoolean("livemode")
        val merchant = json.requiredObject("merchant_info")
        val elementsSession = parseElementsSession(
            json = json,
            livemode = livemode,
            noPaymentRequired = paymentStatus == CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED,
            amount = checkoutItems.sumOf { group -> group.oneTimePrice.items.sumOf { it.total } },
        )
        val automaticTax = json.optionalObject("automatic_tax")
        val savedOptions = json.requiredObject("saved_payment_method_options")
        val paymentResponse = json.optionalObject("payment_response")?.let { response ->
            CheckoutSessionResponse.PaymentResponse(
                status = response.requiredString("status"),
                paymentIntent = response.optionalObject("payment_intent")?.let(::parseIntentReference),
                setupIntent = response.optionalObject("setup_intent")?.let(::parseIntentReference),
                error = response.optionalObject("last_payment_error")?.let {
                    StripeErrorJsonParser().parse(JSONObject().put("error", it))
                },
            )
        }

        CheckoutSessionResponse(
            id = id,
            clientSecret = clientSecret,
            configId = configId,
            automaticPaymentMethodTypes = automaticPaymentMethodTypes,
            currency = currency,
            integrationCurrency = json.requiredString("integration_currency"),
            paymentStatus = paymentStatus,
            status = status,
            livemode = livemode,
            customerEmail = StripeJsonUtils.optString(json, "customer_email"),
            elementsSession = elementsSession,
            paymentIntent = null,
            setupIntent = null,
            paymentResponse = paymentResponse,
            customer = json.optionalObject("customer")?.let {
                parseCustomer(it, savedOptions.optString("payment_method_remove") == "enabled")
            },
            savedPaymentMethodsOfferSave = CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
                enabled = savedOptions.optString("payment_method_save") == "enabled",
            ),
            checkoutItems = checkoutItems,
            totalSummary = parseTotalSummary(json.requiredObject("total_summary")),
            adaptivePricingInfo = json.optionalObject("adaptive_pricing_info")?.let {
                parseAdaptivePricingInfo(it, currency)
            },
            taxMeta = automaticTax?.let {
                CheckoutSessionResponse.TaxMeta(
                    computationType = if (it.requiredBoolean("enabled")) {
                        CheckoutSessionResponse.TaxComputationType.AUTOMATIC
                    } else {
                        CheckoutSessionResponse.TaxComputationType.OFF
                    },
                    status = when (it.optionalString("status")) {
                        "ready" -> CheckoutSessionResponse.TaxStatus.COMPLETE
                        "requires_address" -> CheckoutSessionResponse.TaxStatus.REQUIRES_LOCATION_INPUTS
                        else -> null
                    },
                )
            },
            automaticTaxEnabled = automaticTax?.requiredBoolean("enabled") ?: false,
            taxAddressSource = parseTaxAddressSource(automaticTax),
            allowedShippingCountries = json.optionalObject("shipping_address_collection")
                ?.requiredArray("allowed_countries")?.strings(),
            requiresShippingAddress = json.optionalObject("shipping_address_collection") != null,
            requiresBillingAddress = json.optString("billing_address_collection") == "required",
            merchantCountry = merchant.requiredString("country"),
            businessName = merchant.optionalString("display_name"),
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
        val unitAmountDecimal = json.optionalString("unit_amount_decimal")?.let {
            it.toDoubleOrNull() ?: error("Invalid unit_amount_decimal")
        }?.also {
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
        val amounts = json.requiredObject("amount_details")
        return CheckoutSessionResponse.OneTimePriceItem(
            innerItemKey = json.requiredString("inner_item_key"),
            price = price,
            quantity = json.requiredInt("quantity").also { require(it >= 0) },
            subtotal = amounts.requiredLong("subtotal").also { require(it >= 0) },
            total = amounts.requiredLong("total").also { require(it >= 0) },
            unitAmount = unitAmount,
            unitAmountDecimal = unitAmountDecimal,
            unitLabel = json.optionalString("unit_label"),
            taxAmounts = amounts.requiredArray("tax_amounts").objects().map(::parseTaxAmount),
            taxInclusive = amounts.requiredLong("tax_inclusive").also { require(it >= 0) },
            taxExclusive = amounts.requiredLong("tax_exclusive").also { require(it >= 0) },
            adjustableQuantity = adjustableQuantity,
        )
    }

    private fun parseTotalSummary(totalSummary: JSONObject) = CheckoutSessionResponse.TotalSummary(
        totalTaxAmounts = totalSummary.requiredArray("total_tax_amounts").objects().map(::parseTaxAmount),
    )

    private fun parseTaxAmount(json: JSONObject): CheckoutSessionResponse.TaxAmount {
        val taxRate = json.optionalObject("tax_rate") ?: json
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
                displayName = taxRate.getString("display_name"),
                percentage = taxRate.requiredFiniteDouble("percentage"),
                rateType = rateType,
            ),
        )
    }

    private fun parseAdaptivePricingInfo(
        json: JSONObject,
        currency: String,
    ): CheckoutSessionResponse.AdaptivePricingInfo {
        val integration = json.requiredObject("integration_currency_option")
        return CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = currency,
            integrationAmount = integration.requiredLong("amount").also { require(it >= 0) },
            integrationCurrency = integration.requiredString("currency"),
            integrationDisclosureCaption = integration.optionalObject("disclosure")?.optionalString("caption"),
            localCurrencyOptions = json.requiredArray("local_currency_options").objects().map { option ->
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = option.requiredLong("amount").also { require(it >= 0) },
                    currency = option.requiredString("currency"),
                    disclosureCaption = option.optionalObject("disclosure")?.optionalString("caption"),
                    disclosureDetails = option.optionalObject("disclosure")?.optionalString("details"),
                )
            },
        )
    }

    private fun parseTaxAddressSource(json: JSONObject?): CheckoutSessionResponse.TaxAddressSource? {
        val raw = json?.optionalString("address_source")
        return when (raw) {
            "shipping" -> CheckoutSessionResponse.TaxAddressSource.SHIPPING
            "billing" -> CheckoutSessionResponse.TaxAddressSource.BILLING
            else -> null
        }
    }

    private fun parseCustomer(json: JSONObject, canRemove: Boolean): CheckoutSessionResponse.Customer {
        return CheckoutSessionResponse.Customer(
            id = json.requiredString("id"),
            email = StripeJsonUtils.optString(json, "email"),
            paymentMethods = json.requiredArray("payment_methods").objects().map { paymentMethod ->
                PaymentMethodJsonParser().parse(paymentMethod) ?: error("Invalid payment method")
            },
            canDetachPaymentMethod = canRemove,
        )
    }

    private fun parseIntentReference(json: JSONObject) = CheckoutSessionResponse.IntentReference(
        id = json.requiredString("id"),
        clientSecret = json.requiredString("client_secret"),
    )

    @Suppress("LongMethod")
    private fun parseElementsSession(
        json: JSONObject,
        livemode: Boolean,
        noPaymentRequired: Boolean,
        amount: Long,
    ): ElementsSession {
        val sessionId = json.requiredString("elements_session_id")
        val merchant = json.requiredObject("merchant_info")
        val orderedTypes = json.requiredArray("ordered_payment_method_types_and_wallets").strings()
        val paymentTypes = orderedTypes.filter { PaymentMethod.Type.fromCode(it) != null }
        val linkSettings = json.optionalObject("link_settings")
        val fundingSources = linkSettings?.optJSONArray("link_funding_sources")
        val intentJson = JSONObject()
            .put("payment_method_types", JSONArray(paymentTypes))
            .put("unactivated_payment_method_types", json.requiredArray("unactivated_payment_method_types"))
            .put("link_funding_sources", fundingSources)
            .put("country_code", json.optionalString("country_code"))
        val currency = json.requiredString("presentment_currency")
        val options = json.optionalObject("payment_method_options") ?: JSONObject()
        json.optionalObject("setup_future_usage_for_payment_method_type")?.let { usage ->
            usage.keys().forEach { type ->
                val methodOptions = options.optJSONObject(type) ?: JSONObject()
                methodOptions.put("setup_future_usage", usage.getString(type))
                options.put(type, methodOptions)
            }
        }
        val stripeIntent = if (noPaymentRequired) {
            DeferredSetupIntentJsonParser(
                elementsSessionId = sessionId,
                setupMode = DeferredIntentParams.Mode.Setup(currency, StripeIntent.Usage.OffSession),
                isLiveMode = livemode,
                timeProvider = System::currentTimeMillis,
            ).parse(intentJson).copy(paymentMethodOptionsJsonString = options.toString())
        } else {
            DeferredPaymentIntentJsonParser(
                elementsSessionId = sessionId,
                paymentMode = DeferredIntentParams.Mode.Payment(
                    amount = amount,
                    currency = currency,
                    setupFutureUsage = null,
                    captureMethod = PaymentIntent.CaptureMethod.Automatic,
                    paymentMethodOptionsJsonString = options.toString(),
                ),
                isLiveMode = livemode,
                timeProvider = System::currentTimeMillis,
            ).parse(intentJson)
        }
        val captcha = json.optionalObject("passive_captcha")
        val flags = json.optionalObject("flags")?.let { ElementsSessionJsonParser.parseSessionFlags(it) } ?: emptyMap()
        return ElementsSession(
            linkSettings = parseCheckoutLinkSettings(linkSettings, fundingSources),
            stripeIntent = stripeIntent,
            customer = null,
            merchantCountry = merchant.requiredString("country"),
            merchantLogoUrl = merchant.optionalString("logo_url"),
            cardBrandChoice = ElementsSessionJsonParser.parseCardBrandChoice(json),
            isGooglePayEnabled = json.optString("google_pay_preference") != "disabled",
            externalPaymentMethodData = null,
            customPaymentMethods = emptyList(),
            flags = flags,
            experimentsData = json.optionalObject("experiments_data")?.let {
                ElementsSession.ExperimentsData(
                    arbId = it.optString("arb_id"),
                    experimentAssignments = it.optJSONObject("experiment_assignments")?.let { assignments ->
                        ElementsSessionJsonParser.parseExperimentAssignments(assignments)
                    } ?: emptyMap(),
                )
            },
            orderedPaymentMethodTypesAndWallets = orderedTypes,
            elementsSessionId = sessionId,
            passiveCaptcha = captcha?.let {
                PassiveCaptchaParams(
                    siteKey = it.requiredString("site_key"),
                    rqData = it.optionalString("rqdata"),
                    tokenTimeoutSeconds = it.optionalInt("token_refresh_after_seconds"),
                )
            },
            elementsSessionConfigId = json.optionalString("elements_session_config_id"),
            accountId = null,
            merchantId = null,
        )
    }

    private fun parseCheckoutLinkSettings(
        json: JSONObject?,
        fundingSources: JSONArray?,
    ): ElementsSession.LinkSettings {
        val settings = ElementsSessionJsonParser.parseLinkSettings(json, fundingSources)
        return if (json?.has("link_passthrough_mode_enabled") == true) {
            settings
        } else {
            settings.copy(
                linkPassthroughModeEnabled = settings.linkMode == LinkMode.Passthrough ||
                    settings.linkMode == LinkMode.LinkCardBrand,
            )
        }
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
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
}
