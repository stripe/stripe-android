package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class CheckoutSessionResponseJsonParserTest {
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
        val first = json.getJSONArray("checkout_items").getJSONObject(0)
        val second = JSONObject(first.toString()).put("key", "group_2")
        second.getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
            .put("inner_item_key", "item_2")
            .put("tax_amounts", JSONArray().put(taxAmount()))
        json.getJSONArray("checkout_items").put(second)

        val result = parse(json)!!

        assertThat(result.checkoutItems).hasSize(2)
        assertThat(result.checkoutItems[1].oneTimePrice.items.single().taxAmounts.single().taxRate.displayName)
            .isEqualTo("Sales tax")
    }

    @Test
    fun `parses aggregate discounts and taxes separately`() {
        val json = base()
        json.getJSONObject("recurring_details")
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

        assertThat(result.recurringDetails?.totalTaxAmounts).hasSize(1)
        assertThat(result.recurringDetails?.totalDiscountAmounts?.single()?.promotionCode?.code).isEqualTo("SAVE10")
    }

    @Test
    fun `parses adaptive pricing`() {
        val json = base().put(
            "adaptive_pricing_info",
            JSONObject().put("active_presentment_currency", "usd").put("integration_amount", 5000)
                .put("integration_currency", "cad")
                .put(
                    "local_currency_options",
                    JSONArray().put(
                        JSONObject().put("amount", 5099).put("conversion_markup_bps", 400)
                            .put("currency", "usd").put("presentment_exchange_rate", "0.90")
                    )
                )
        )

        val result = parse(json)!!

        assertThat(result.adaptivePricingInfo?.integrationCurrency).isEqualTo("cad")
        assertThat(result.adaptivePricingInfo?.activePresentmentCurrency).isEqualTo("usd")
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
    fun `parses confirmed payment response and replaces deferred intent`() {
        val result = parseResponse(baseFixture = "checkout-session-confirm.json")
        val paymentIntent = requireNotNull(result.paymentIntent)
        val elementsSession = requireNotNull(result.elementsSession)

        assertThat(result.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(result.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.PAID)
        assertThat(paymentIntent.id).isEqualTo("pi_cs_example")
        assertThat(paymentIntent.status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(result.setupIntent).isNull()
        assertThat(elementsSession.stripeIntent).isEqualTo(paymentIntent)
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
        assertThat(offerSave.status)
            .isEqualTo(CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED)
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
            { it.put("mode", "payment") },
            { it.put("checkout_items", JSONArray()) },
            { it.getJSONArray("checkout_items").getJSONObject(0).put("type", "subscription") },
            { item(it).getJSONObject("price").put("currency", "eur") },
            { it.put("status", "unknown") },
            { it.put("payment_status", "unknown") },
            { item(it).remove("inner_item_key") },
            { item(it).put("unit_amount_decimal", "NaN") },
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
    fun `rejects malformed tax rate`() {
        val json = base()
        item(json).put(
            "tax_amounts",
            JSONArray().put(
                taxAmount().apply {
                    getJSONObject("tax_rate").put("rate_type", "mystery")
                }
            )
        )

        assertThat(parse(json)).isNull()
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

    private fun item(json: JSONObject): JSONObject = json.getJSONArray("checkout_items")
        .getJSONObject(0).getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)

    private fun taxAmount() = JSONObject().put("amount", 80).put("inclusive", false).put(
        "tax_rate",
        JSONObject().put("display_name", "Sales tax").put("percentage", 8.0).put("rate_type", "percentage")
    )

    private fun parse(json: JSONObject) = CheckoutSessionResponseJsonParser.parse(json)
}
