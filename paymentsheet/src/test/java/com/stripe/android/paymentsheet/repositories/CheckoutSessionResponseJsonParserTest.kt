package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CheckoutSessionResponseJsonParserTest {
    @Test
    fun `parses nested amounts without legacy flat amounts`() {
        val json = base()
        val item = item(json)
        listOf("subtotal", "total", "tax_amounts", "tax_inclusive", "tax_exclusive").forEach(item::remove)
        item.getJSONObject("amount_details").put("subtotal", 4000).put("total", 4080)
            .put("tax_exclusive", 80).put("tax_amounts", JSONArray().put(taxAmount()))

        val result = requireNotNull(parse(json)).checkoutItems.single().oneTimePrice.items.single()

        assertThat(result.subtotal).isEqualTo(4000)
        assertThat(result.total).isEqualTo(4080)
        assertThat(result.taxExclusive).isEqualTo(80)
        assertThat(result.taxAmounts.single().amount).isEqualTo(80)
    }

    @Test
    fun `parses independent integration currency without adaptive pricing`() {
        val json = base().put("integration_currency", "eur")
        json.remove("adaptive_pricing_info")

        val result = requireNotNull(parse(json))

        assertThat(result.integrationCurrency).isEqualTo("eur")
        assertThat(result.currency).isEqualTo("usd")
    }

    @Test
    fun `rejects an intent reference without its client secret`() {
        val json = response("checkout-session-confirm.json")
        json.getJSONObject("payment_response").getJSONObject("payment_intent").remove("client_secret")

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `rejects a payment response without status`() {
        val json = response("checkout-session-confirm.json")
        json.getJSONObject("payment_response").remove("status")

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `preserves localized disclosure details`() {
        val json = response("checkout-session-adaptive-pricing-default.json")
        json.getJSONObject("adaptive_pricing_info").getJSONArray("local_currency_options").getJSONObject(0)
            .put("disclosure", JSONObject().put("caption", "Rate").put("details", "Guaranteed rate details"))

        val result = requireNotNull(parse(json))

        assertThat(result.adaptivePricingInfo?.localCurrencyOptions?.single()?.disclosureDetails)
            .isEqualTo("Guaranteed rate details")
    }

    @Test
    fun `derives passthrough from checkout Link mode`(
        @TestParameter(
            value = ["PASSTHROUGH", "LINK_CARD_BRAND", "LINK_PAYMENT_METHOD", "unknown", "null"]
        ) mode: String?,
    ) {
        val json = base()
        json.getJSONObject("link_settings").apply {
            remove("link_passthrough_mode_enabled")
            put("link_mode", mode)
        }
        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.linkSettings?.linkPassthroughModeEnabled)
            .isEqualTo(mode == "PASSTHROUGH" || mode == "LINK_CARD_BRAND")
    }

    @Test
    fun `preserves explicit legacy passthrough flag`(
        @TestParameter(
            value = ["PASSTHROUGH", "LINK_CARD_BRAND", "LINK_PAYMENT_METHOD", "unknown", "null"]
        ) mode: String?,
        @TestParameter enabled: Boolean,
    ) {
        val json = base()
        json.getJSONObject("link_settings")
            .put("link_mode", mode)
            .put("link_passthrough_mode_enabled", enabled)

        assertThat(parse(json)?.elementsSession?.linkSettings?.linkPassthroughModeEnabled).isEqualTo(enabled)
    }

    @Test
    fun `card brand Link is enabled without a Link payment method`() {
        val json = base().put("ordered_payment_method_types_and_wallets", JSONArray(listOf("card")))
        json.getJSONObject("link_settings").put("link_mode", "LINK_CARD_BRAND")
            .remove("link_passthrough_mode_enabled")
        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.stripeIntent.paymentMethodTypes).doesNotContain("link")
        assertThat(elements.isLinkEnabled).isTrue()
        assertThat(elements.linkSettings?.linkPassthroughModeEnabled).isTrue()
    }

    @Test
    fun `rejects custom ui mode`() {
        val json = base()
        assertThat(parse(json)).isNotNull()

        json.put("ui_mode", "custom")

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `parses customer email as fixed email`() {
        val json = base().put(
            "customer",
            JSONObject().put("id", "cus_test").put("email", "customer@example.com")
                .put("payment_methods", JSONArray()),
        )
        val result = requireNotNull(parse(json))
        assertThat(result.customer?.email).isEqualTo("customer@example.com")
        assertThat(result.fixedEmail).isEqualTo("customer@example.com")
    }

    @Test
    fun `session email takes precedence over customer email`() {
        val json = base().put("customer_email", "session@example.com").put(
            "customer",
            JSONObject().put("id", "cus_test").put("email", "customer@example.com")
                .put("payment_methods", JSONArray()),
        )
        val result = requireNotNull(parse(json))
        assertThat(result.fixedEmail).isEqualTo("session@example.com")
    }

    @Test
    fun `parses nested unified one-time price group`() {
        val result = parse(base())

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_abc123")
        assertThat(result?.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.UNPAID)
        assertThat(result?.requiresShippingAddress).isFalse()
        assertThat(result?.checkoutItems).hasSize(1)
        val group = result!!.checkoutItems.single()
        assertThat(group.key).isEqualTo("group_1")
        assertThat(group.oneTimePrice.items.single().innerItemKey).isEqualTo("item_1")
        assertThat(group.oneTimePrice.items.single().subtotal).isEqualTo(5099)
    }

    @Test
    fun `checkout config id is null when the response omits it`() {
        val json = base().apply { remove("checkout_session_config_id") }
        val result = requireNotNull(parse(json))

        assertThat(result.configId).isNull()
    }

    @Test
    fun `parses checkout config id separately from elements session config id`() {
        val json = base().put("checkout_session_config_id", "cfg_checkout_123")
        json.put("elements_session_config_id", "cfg_elements_123")
        val result = requireNotNull(parse(json))

        assertThat(result.configId).isEqualTo("cfg_checkout_123")
        assertThat(result.elementsSession?.elementsSessionConfigId).isEqualTo("cfg_elements_123")
    }

    @Test
    fun `automatic payment method types is null when omitted`() {
        val result = requireNotNull(parse(base()))

        assertThat(result.automaticPaymentMethodTypes).isNull()
    }

    @Test
    fun `automatic payment method types is null when explicitly null`() {
        val result = requireNotNull(parse(base().put("automatic_payment_method_types", JSONObject.NULL)))

        assertThat(result.automaticPaymentMethodTypes).isNull()
    }

    @Test
    fun `parses automatic payment method types enabled`() {
        val result = requireNotNull(parse(base().put("automatic_payment_method_types", true)))

        assertThat(result.automaticPaymentMethodTypes).isTrue()
    }

    @Test
    fun `parses automatic payment method types disabled`() {
        val result = requireNotNull(parse(base().put("automatic_payment_method_types", false)))

        assertThat(result.automaticPaymentMethodTypes).isFalse()
    }

    @Test
    fun `parses recorded checkout configuration fields`() {
        val result = parseResponse(baseFixture = "checkout-session-init.json")
        val elements = requireNotNull(result.elementsSession)

        assertThat(result.configId).isEqualTo("checkout_config_123")
        assertThat(elements.elementsSessionConfigId).isEqualTo("cs_config_123")
        assertThat(elements.linkSettings?.linkMode).isEqualTo(LinkMode.LinkCardBrand)
        assertThat(result.requiresBillingAddress).isFalse()
    }

    @Test
    fun `shipping address collection requires a shipping address`() {
        val json = base().put(
            "shipping_address_collection",
            JSONObject().put("allowed_countries", JSONArray().put("US").put("CA"))
        )

        val result = parse(json)

        assertThat(result?.allowedShippingCountries).containsExactly("US", "CA").inOrder()
        assertThat(result?.requiresShippingAddress).isTrue()
    }

    @Test
    fun `null shipping address collection does not require a shipping address`() {
        val json = base().put(
            "shipping_address_collection",
            null,
        )

        val result = parse(json)

        assertThat(result?.requiresShippingAddress).isFalse()
    }

    @Test
    fun `unit amount override takes precedence and decimal is retained`() {
        val json = base()
        val item = item(json)
        item.put("unit_amount", 4000)
        item.put("unit_amount_decimal", "4000.123456789012")

        val result = parse(json)!!.checkoutItems.single().oneTimePrice.items.single()

        assertThat(result.unitAmount).isEqualTo(4000)
        assertThat(result.price.unitAmount).isEqualTo(5099)
        assertThat(result.unitAmountDecimal).isEqualTo(4000.123456789012)
    }

    @Test
    fun `parses multiple groups and per-item tax`() {
        val json = base()
        val first = json.getJSONArray("items").getJSONObject(0)
        val second = JSONObject(first.toString()).put("key", "group_2")
        second.getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
            .put("inner_item_key", "item_2")
            .getJSONObject("amount_details")
            .put("tax_amounts", JSONArray().put(taxAmount()))
        json.getJSONArray("items").put(second)

        val result = parse(json)!!

        assertThat(result.checkoutItems).hasSize(2)
        assertThat(result.checkoutItems[1].oneTimePrice.items.single().taxAmounts.single().taxRate.displayName)
            .isEqualTo("Sales tax")
    }

    @Test
    fun `parses aggregate discounts and taxes from recurring total summary`() {
        val json = base()
        json.getJSONObject("total_summary")
            .put("total_tax_amounts", JSONArray().put(taxAmount()))
            .put(
                "total_discount_amounts",
                JSONArray().put(
                    JSONObject().put("amount", 500).put("display_name", "Summer")
                        .put("coupon", JSONObject().put("code", "SUMMER").put("name", "Summer").put("percent_off", 10))
                        .put("promotion_code", JSONObject().put("code", "SAVE10"))
                )
            )

        val result = parse(json)!!

        assertThat(result.totalSummary?.totalTaxAmounts).hasSize(1)
    }

    @Test
    fun `ignores deprecated recurring amount fields`() {
        val json = base()
        json.put("recurring_details", JSONObject())
            .getJSONObject("recurring_details")
            .put("total_tax_amounts", JSONArray().put(taxAmount()))
            .put("total_discount_amounts", JSONArray().put(JSONObject().put("amount", 500)))

        val result = requireNotNull(parse(json))

        assertThat(result.totalSummary?.totalTaxAmounts).isEmpty()
    }

    @Test
    fun `rejects recurring details without total summary`() {
        val json = base()
        assertThat(parse(json)).isNotNull()
        json.remove("total_summary")

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `parses adaptive pricing`() {
        val json = base().put(
            "adaptive_pricing_info",
            JSONObject().put("integration_currency_option", JSONObject().put("amount", 5000).put("currency", "cad"))
                .put(
                    "local_currency_options",
                    JSONArray().put(
                        JSONObject().put("amount", 5099).put("currency", "usd")
                            .put("disclosure", JSONObject().put("caption", "Localized disclosure"))
                    )
                )
        )

        val result = parse(json)!!

        assertThat(result.adaptivePricingInfo?.integrationCurrency).isEqualTo("cad")
        assertThat(result.adaptivePricingInfo?.activePresentmentCurrency).isEqualTo("usd")
    }

    @Test
    fun `parses adaptive pricing fixture with distinct integration and presentment currencies`() {
        val result = parseResponse(baseFixture = "checkout-session-adaptive-pricing-default.json")
        val pricing = requireNotNull(result.adaptivePricingInfo)

        assertThat(result.currency).isEqualTo("eur")
        assertThat(pricing.integrationCurrency).isEqualTo("usd")
        assertThat(pricing.integrationAmount).isEqualTo(5099)
        assertThat(pricing.activePresentmentCurrency).isEqualTo("eur")
        assertThat(pricing.localCurrencyOptions.single().amount).isEqualTo(4594)
        assertThat(pricing.integrationDisclosureCaption)
            .isEqualTo("Exchange rate and fees of your bank may apply.")
        val intent = result.elementsSession?.stripeIntent as PaymentIntent
        assertThat(intent.currency).isEqualTo("eur")
        assertThat(result.checkoutItems.single().oneTimePrice.items.single().price.currency).isEqualTo("eur")
    }

    @Test
    fun `parses every status and payment status combination`() {
        CheckoutSessionResponse.Status.entries.forEach { status ->
            CheckoutSessionResponse.PaymentStatus.entries.forEach { paymentStatus ->
                val json = base()
                    .put("status", status.name.lowercase())
                    .put("payment_status", paymentStatus.name.lowercase())
                if (paymentStatus == CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED) {
                    json.put("payment_status", "no_payment_required")
                }
                assertThat(parse(json)).isNotNull()
            }
        }
    }

    @Test
    fun `no payment required produces setup-style elements session`() {
        val json = base().put("payment_status", "no_payment_required")

        val result = parse(json)

        assertThat(result?.elementsSession?.stripeIntent).isInstanceOf(SetupIntent::class.java)
    }

    @Test
    fun `parses confirmation intent references without fabricating confirmed intents`() {
        val result = parseResponse(baseFixture = "checkout-session-confirm.json")

        assertThat(result.paymentResponse?.paymentIntent?.id).isEqualTo("pi_checkout123")
        assertThat(result.paymentResponse?.paymentIntent?.clientSecret).isNotEmpty()
        assertThat(result.paymentIntent).isNull()
        assertThat(result.elementsSession?.stripeIntent?.clientSecret).isNull()
    }

    @Test
    fun `parses confirmed payment response when customer detach capability is absent`() {
        val result = parseResponse(
            baseFixture = "checkout-session-confirm.json",
            overridesFixture = "checkout-session-customer-without-detach-capability.json",
        )
        val customer = requireNotNull(result.customer)

        assertThat(customer.id).isEqualTo("cus_VDr7KDndDhelXY")
        assertThat(customer.paymentMethods).isEmpty()
        assertThat(customer.canDetachPaymentMethod).isFalse()
    }

    @Test
    fun `parses returning customer with saved payment methods`() {
        val result = parseResponse(
            baseFixture = "checkout-session-init.json",
            overridesFixture = "checkout-session-returning-customer.json",
        )
        val customer = requireNotNull(result.customer)
        val offerSave = requireNotNull(result.savedPaymentMethodsOfferSave)

        assertThat(result.customerEmail).isEqualTo("email@example.com")
        assertThat(customer.id).isEqualTo("cus_VDrEHIySBXFE9z")
        assertThat(customer.canDetachPaymentMethod).isTrue()
        assertThat(customer.paymentMethods.map { it.id })
            .containsExactly("pm_1UDPW3Lu5o3P18Zp5rGujYbm", "pm_1UDPW3Lu5o3P18ZpfZ3q9HVI")
            .inOrder()
        assertThat(customer.paymentMethods.map { it.type })
            .containsExactly(PaymentMethod.Type.USBankAccount, PaymentMethod.Type.Card)
            .inOrder()
        assertThat(offerSave.enabled).isTrue()
    }

    @Test
    fun `parses automatic tax requiring billing location`() {
        val result = parseResponse(
            baseFixture = "checkout-session-init.json",
            overridesFixture = "checkout-session-automatic-tax.json",
        )
        val taxMeta = requireNotNull(result.taxMeta)

        assertThat(result.automaticTaxEnabled).isTrue()
        assertThat(result.taxAddressSource).isEqualTo(CheckoutSessionResponse.TaxAddressSource.BILLING)
        assertThat(result.requiresBillingAddress).isTrue()
        assertThat(result.collectsTaxFromBillingAddress).isTrue()
        assertThat(taxMeta.computationType)
            .isEqualTo(CheckoutSessionResponse.TaxComputationType.AUTOMATIC)
        assertThat(taxMeta.status)
            .isEqualTo(CheckoutSessionResponse.TaxStatus.REQUIRES_LOCATION_INPUTS)
    }

    @Test
    fun `rejects legacy and malformed response boundaries`() {
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.remove("ui_mode") },
            { it.remove("id") },
            { it.getJSONObject("merchant_info").remove("country") },
            { it.remove("ordered_payment_method_types_and_wallets") },
            { it.put("items", JSONArray()) },
            { it.getJSONArray("items").getJSONObject(0).put("type", "subscription") },
            { item(it).getJSONObject("price").put("currency", "eur") },
            { it.put("status", "unknown") },
            { it.put("payment_status", "unknown") },
            { item(it).remove("inner_item_key") },
            { item(it).put("unit_amount_decimal", "NaN") },
            { item(it).put("unit_amount_decimal", "abc") },
            { it.remove("elements_session_id") },
        )

        mutations.forEach { mutation ->
            assertThat(parse(base().also(mutation))).isNull()
        }
    }

    @Test
    fun `rejects enabled adjustable quantity without bounds`() {
        val json = base()
        item(json).put("adjustable_quantity", JSONObject().put("enabled", true))

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `parses tax rate with empty display name`() {
        val json = base()
        item(json).getJSONObject("amount_details").put(
            "tax_amounts",
            JSONArray().put(
                taxAmount().apply {
                    getJSONObject("tax_rate").put("display_name", "")
                }
            )
        )

        val result = requireNotNull(parse(json))
        val taxRate = result.checkoutItems.single().oneTimePrice.items.single().taxAmounts.single().taxRate

        assertThat(taxRate.displayName).isEmpty()
        assertThat(taxRate.percentage).isEqualTo(8.0)
        assertThat(taxRate.rateType).isEqualTo(CheckoutSessionResponse.TaxRateType.PERCENTAGE)
    }

    @Test
    fun `rejects malformed tax rate`() {
        val json = base()
        item(json).getJSONObject("amount_details").put(
            "tax_amounts",
            JSONArray().put(
                taxAmount().apply {
                    getJSONObject("tax_rate").put("rate_type", "mystery")
                }
            )
        )

        assertThat(parse(json)).isNull()
    }

    @Test
    fun `mode is not validated`() {
        assertThat(parse(base())).isNotNull()
        assertThat(parse(base().put("mode", "payment"))).isNotNull()
    }

    @Test
    fun `constructs checkout elements configuration from top level fields`() {
        val json = base()
            .put(
                "ordered_payment_method_types_and_wallets",
                JSONArray(listOf("cashapp", "google_pay", "card", "link", "blik", "vipps")),
            )
            .put("google_pay_preference", "disabled")
            .put(
                "setup_future_usage_for_payment_method_type",
                JSONObject().put("card", "off_session").put("cashapp", "none"),
            )
            .put("payment_method_options", JSONObject().put("card", JSONObject().put("capture_method", "manual")))
            .put(
                "passive_captcha",
                JSONObject().put("site_key", "site").put("rqdata", "rq").put("token_refresh_after_seconds", 30),
            )
            .put("flags", JSONObject().put("elements_enable_passive_captcha", true))
        val result = requireNotNull(parse(json))
        val elements = requireNotNull(result.elementsSession)
        val intent = elements.stripeIntent as PaymentIntent

        assertThat(result.clientSecret).isEqualTo("cs_test_abc123_secret_test")
        assertThat(elements.orderedPaymentMethodTypesAndWallets)
            .containsExactly("cashapp", "google_pay", "card", "link", "blik", "vipps").inOrder()
        assertThat(intent.paymentMethodTypes).containsExactly("cashapp", "card", "link", "blik").inOrder()
        assertThat(intent.amount).isEqualTo(5099)
        assertThat(intent.getPaymentMethodOptions()["card"])
            .isEqualTo(mapOf("capture_method" to "manual", "setup_future_usage" to "off_session"))
        assertThat(intent.getPaymentMethodOptions()["cashapp"]).isEqualTo(mapOf("setup_future_usage" to "none"))
        assertThat(elements.isGooglePayEnabled).isFalse()
        assertThat(elements.isLinkEnabled).isTrue()
        assertThat(elements.merchantCountry).isEqualTo("US")
        assertThat(elements.passiveCaptchaParams?.siteKey).isEqualTo("site")
        assertThat(elements.passiveCaptchaParams?.tokenTimeoutSeconds).isEqualTo(30)
    }

    @Test
    fun `omits absent localized disclosure`() {
        val json = base()
        json.put(
            "adaptive_pricing_info",
            JSONObject()
                .put("integration_currency_option", JSONObject().put("currency", "usd").put("amount", 5099))
                .put(
                    "local_currency_options",
                    JSONArray().put(JSONObject().put("currency", "eur").put("amount", 4500)),
                ),
        )

        val result = requireNotNull(parse(json))

        assertThat(result.adaptivePricingInfo?.localCurrencyOptions?.single()?.disclosureCaption).isNull()
    }

    @Test
    fun `allows absent elements session config id`() {
        val json = base().apply { remove("elements_session_config_id") }

        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.elementsSessionId).isEqualTo(json.getString("elements_session_id"))
        assertThat(elements.elementsSessionConfigId).isNull()
    }

    @Test
    fun `ignores passive captcha when its flag is disabled`() {
        val json = base()
            .put("passive_captcha", JSONObject().put("site_key", "site"))
            .put("flags", JSONObject().put("elements_enable_passive_captcha", false))

        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.passiveCaptchaParams).isNull()
    }

    @Test
    fun `ignores passive captcha without its flag`() {
        val json = base()
            .put("passive_captcha", JSONObject().put("site_key", "site"))
            .apply { remove("flags") }

        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.passiveCaptchaParams).isNull()
    }

    @Test
    fun `parses unknown automatic tax status without a tax status`() {
        val json = base().put(
            "automatic_tax",
            JSONObject().put("enabled", true).put("status", "failed").put("address_source", "billing"),
        )

        val taxMeta = requireNotNull(parse(json)?.taxMeta)

        assertThat(taxMeta.computationType).isEqualTo(CheckoutSessionResponse.TaxComputationType.AUTOMATIC)
        assertThat(taxMeta.status).isNull()
    }

    @Test
    fun `deferred payment intent amount uses item totals instead of total due`() {
        val json = base()
        json.getJSONObject("total_summary").put("due", 1)

        val result = requireNotNull(parse(json))
        val intent = result.elementsSession?.stripeIntent as PaymentIntent

        assertThat(intent.amount).isEqualTo(result.amount)
        assertThat(intent.amount).isNotEqualTo(1L)
    }

    @Test
    fun `preserves checkout flags experiments and card brand choice`() {
        val json = base()
            .put("flags", JSONObject().put("elements_enable_link_spm", true))
            .put(
                "experiments_data",
                JSONObject().put("arb_id", "arb123")
                    .put("experiment_assignments", JSONObject().put("link_global_holdback", "treatment")),
            )
            .put(
                "card_brand_choice",
                JSONObject().put("eligible", true).put("preferred_networks", JSONArray(listOf("cartes_bancaires"))),
            )
        val elements = requireNotNull(parse(json)?.elementsSession)

        assertThat(elements.enableLinkInSpm).isTrue()
        assertThat(elements.experimentsData?.arbId).isEqualTo("arb123")
        assertThat(elements.experimentsData?.experimentAssignments)
            .containsEntry(ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK, "treatment")
        assertThat(elements.cardBrandChoice?.eligible).isTrue()
        assertThat(elements.cardBrandChoice?.preferredNetworks).containsExactly("cartes_bancaires")
    }

    private fun base(): JSONObject = response(baseFixture = "checkout-session-init.json")

    private fun parseResponse(
        baseFixture: String,
        overridesFixture: String? = null,
    ): CheckoutSessionResponse {
        return requireNotNull(parse(response(baseFixture, overridesFixture)))
    }

    private fun response(
        baseFixture: String,
        overridesFixture: String? = null,
    ): JSONObject {
        val response = jsonFixture(baseFixture)
        overridesFixture?.let(::jsonFixture)?.let { overrides ->
            overrides.keys().forEach { key -> response.put(key, overrides.get(key)) }
        }
        return response
    }

    private fun jsonFixture(name: String): JSONObject {
        val resource = requireNotNull(javaClass.classLoader?.getResource(name))
        return JSONObject(resource.readText())
    }

    private fun item(json: JSONObject): JSONObject = json.getJSONArray("items")
        .getJSONObject(0).getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)

    private fun taxAmount() = JSONObject().put("amount", 80).put("inclusive", false).put(
        "tax_rate",
        JSONObject().put("display_name", "Sales tax").put("percentage", 8.0).put("rate_type", "percentage")
    )

    private fun parse(json: JSONObject) = CheckoutSessionResponseJsonParser("cs_test_abc123_secret_test").parse(json)
}
