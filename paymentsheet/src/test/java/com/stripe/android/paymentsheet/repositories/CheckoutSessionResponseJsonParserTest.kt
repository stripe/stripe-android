package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionResponseJsonParserTest {

    @Test
    fun `parse checkout session response`() {
        val result = CheckoutSessionResponseJsonParser(
            isLiveMode = false,
        ).parse(CheckoutSessionFixtures.CHECKOUT_SESSION_RESPONSE_JSON)

        // Verify CheckoutSessionResponse fields
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
        assertThat(result?.amount).isEqualTo(999L)
        assertThat(result?.currency).isEqualTo("usd")

        // Verify ElementsSession is parsed correctly
        val elementsSession = result?.elementsSession
        assertThat(elementsSession).isNotNull()
        assertThat(elementsSession?.elementsSessionId).isEqualTo("elements_session_1nWWJQ3A6yS")
        assertThat(elementsSession?.merchantCountry).isEqualTo("US")
        assertThat(elementsSession?.isGooglePayEnabled).isTrue()

        // Verify StripeIntent is created correctly
        val stripeIntent = elementsSession?.stripeIntent
        assertThat(stripeIntent).isNotNull()
        assertThat(stripeIntent).isInstanceOf(PaymentIntent::class.java)

        // Verify payment method types from ordered_payment_method_types
        assertThat(stripeIntent?.paymentMethodTypes).containsExactly(
            "card",
            "link",
            "cashapp",
            "alipay",
            "wechat_pay",
            "us_bank_account",
            "amazon_pay",
            "afterpay_clearpay",
            "klarna",
            "crypto"
        ).inOrder()
    }

    @Test
    fun `parse checkout session response includes line items`() {
        val result = CheckoutSessionResponseJsonParser(
            isLiveMode = false,
        ).parse(CheckoutSessionFixtures.CHECKOUT_SESSION_RESPONSE_JSON)

        assertThat(result).isNotNull()
        val lineItems = result!!.lineItems
        assertThat(lineItems).hasSize(1)
        assertThat(lineItems[0].id).isEqualTo("li_1SrjAuLu5o3P18ZpVBMMs98l")
        assertThat(lineItems[0].name).isEqualTo("Llama Figure")
        assertThat(lineItems[0].quantity).isEqualTo(1)
        assertThat(lineItems[0].subtotal).isEqualTo(999L)
        assertThat(lineItems[0].total).isEqualTo(999L)
        assertThat(lineItems[0].unitAmount).isEqualTo(999L)
    }

    @Test
    fun `parse multiple line items`() {
        val result = CheckoutSessionResponseJsonParser(
            isLiveMode = false,
        ).parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_MULTIPLE_LINE_ITEMS_JSON)

        assertThat(result).isNotNull()
        val lineItems = result!!.lineItems
        assertThat(lineItems).hasSize(2)

        assertThat(lineItems[0].id).isEqualTo("li_item1")
        assertThat(lineItems[0].name).isEqualTo("Llama Figure")
        assertThat(lineItems[0].quantity).isEqualTo(2)
        assertThat(lineItems[0].subtotal).isEqualTo(1998L)
        assertThat(lineItems[0].total).isEqualTo(1998L)
        assertThat(lineItems[0].unitAmount).isEqualTo(999L)

        assertThat(lineItems[1].id).isEqualTo("li_item2")
        assertThat(lineItems[1].name).isEqualTo("Alpaca Plushie")
        assertThat(lineItems[1].quantity).isEqualTo(1)
        assertThat(lineItems[1].subtotal).isEqualTo(2499L)
        assertThat(lineItems[1].total).isEqualTo(2499L)
        assertThat(lineItems[1].unitAmount).isEqualTo(2499L)
    }

    @Test
    fun `parse returns empty line items when no line_item_group`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000, "subtotal": 1000, "total": 1000 }
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result!!.lineItems).isEmpty()
    }

    @Test
    fun `parse returns null when session_id is missing`() {
        val json = JSONObject(
            """
            {
                "currency": "usd",
                "total_summary": { "due": 1000 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when currency is missing`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "total_summary": { "due": 1000 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when total_summary is missing`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse succeeds when elements_session is missing`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000 }
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_123")
        assertThat(result?.amount).isEqualTo(1000L)
        assertThat(result?.currency).isEqualTo("usd")
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse returns null elements_session when it is invalid`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000 },
                "elements_session": {}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        // Response is parsed successfully but elements_session is null due to invalid JSON
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_123")
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse confirm response with succeeded payment intent`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_CONFIRM_SUCCEEDED_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
        assertThat(result?.amount).isEqualTo(999L)
        assertThat(result?.currency).isEqualTo("usd")

        // Verify PaymentIntent is parsed
        val paymentIntent = result?.paymentIntent
        assertThat(paymentIntent).isNotNull()
        assertThat(paymentIntent?.id).isEqualTo("pi_3QWK2VIyGgrkZxL71xfPBWG5")
        assertThat(paymentIntent?.status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(paymentIntent?.isConfirmed).isTrue()

        // Confirm responses don't include elements_session
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse confirm response with requires_action payment intent`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_CONFIRM_REQUIRES_ACTION_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")

        // Verify PaymentIntent has requires_action status
        val paymentIntent = result?.paymentIntent
        assertThat(paymentIntent).isNotNull()
        assertThat(paymentIntent?.id).isEqualTo("pi_3QWK2VIyGgrkZxL71xfPBWG5")
        assertThat(paymentIntent?.status).isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(paymentIntent?.requiresAction()).isTrue()

        // Verify next_action is parsed
        assertThat(paymentIntent?.nextActionType).isEqualTo(StripeIntent.NextActionType.RedirectToUrl)
    }

    @Test
    fun `parse init response with customer and saved payment methods`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_CUSTOMER_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_abc123")
        assertThat(result?.amount).isEqualTo(1000L)

        // Verify customer is parsed from top-level
        val customer = result?.customer
        assertThat(customer).isNotNull()
        assertThat(customer?.id).isEqualTo("cus_test_customer")

        // Verify payment methods are parsed
        val paymentMethods = customer?.paymentMethods
        assertThat(paymentMethods).hasSize(2)
        assertThat(paymentMethods?.get(0)?.id).isEqualTo("pm_card_visa")
        assertThat(paymentMethods?.get(0)?.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(paymentMethods?.get(0)?.card?.last4).isEqualTo("4242")
        assertThat(paymentMethods?.get(0)?.card?.brand).isEqualTo(CardBrand.Visa)

        assertThat(paymentMethods?.get(1)?.id).isEqualTo("pm_card_mastercard")
        assertThat(paymentMethods?.get(1)?.card?.last4).isEqualTo("5555")
        assertThat(paymentMethods?.get(1)?.card?.brand).isEqualTo(CardBrand.MasterCard)
    }

    @Test
    fun `parse init response with customer but empty payment methods`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_EMPTY_CUSTOMER_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.customer).isNotNull()
        assertThat(result?.customer?.id).isEqualTo("cus_test_empty_customer")
        assertThat(result?.customer?.paymentMethods).isEmpty()
    }

    @Test
    fun `parse init response without customer returns null customer`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITHOUT_CUSTOMER_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_abc123")
        assertThat(result?.customer).isNull()
    }

    @Test
    fun `parse init response with save offer enabled and status not_accepted`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_SAVE_ENABLED_JSON)

        assertThat(result).isNotNull()
        val offerSave = result?.savedPaymentMethodsOfferSave
        assertThat(offerSave).isNotNull()
        assertThat(offerSave?.enabled).isTrue()
        assertThat(offerSave?.status).isEqualTo(
            CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED
        )
    }

    @Test
    fun `parse init response with save offer enabled and status accepted`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_SAVE_ACCEPTED_JSON)

        assertThat(result).isNotNull()
        val offerSave = result?.savedPaymentMethodsOfferSave
        assertThat(offerSave).isNotNull()
        assertThat(offerSave?.enabled).isTrue()
        assertThat(offerSave?.status).isEqualTo(
            CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.ACCEPTED
        )
    }

    @Test
    fun `parse init response with save offer disabled`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_SAVE_DISABLED_JSON)

        assertThat(result).isNotNull()
        val offerSave = result?.savedPaymentMethodsOfferSave
        assertThat(offerSave).isNotNull()
        assertThat(offerSave?.enabled).isFalse()
        assertThat(offerSave?.status).isEqualTo(
            CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED
        )
    }

    @Test
    fun `parse init response without save offer returns null`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITHOUT_CUSTOMER_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.savedPaymentMethodsOfferSave).isNull()
    }

    @Test
    fun `parse customer with can_detach_payment_method true`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_abc123",
                "currency": "usd",
                "total_summary": {
                    "due": 1000
                },
                "customer": {
                    "id": "cus_test_customer",
                    "payment_methods": [],
                    "can_detach_payment_method": true
                },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        val customer = result?.customer
        assertThat(customer).isNotNull()
        assertThat(customer?.canDetachPaymentMethod).isTrue()
    }

    @Test
    fun `parse customer with can_detach_payment_method false`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_abc123",
                "currency": "usd",
                "total_summary": {
                    "due": 1000
                },
                "customer": {
                    "id": "cus_test_customer",
                    "payment_methods": [],
                    "can_detach_payment_method": false
                },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        val customer = result?.customer
        assertThat(customer).isNotNull()
        assertThat(customer?.canDetachPaymentMethod).isFalse()
    }

    @Test
    fun `parse customer without can_detach_payment_method defaults to false`() {
        // Use existing fixture that doesn't have can_detach_payment_method field
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_CUSTOMER_JSON)

        assertThat(result).isNotNull()
        val customer = result?.customer
        assertThat(customer).isNotNull()
        assertThat(customer?.canDetachPaymentMethod).isFalse()
    }

    @Test
    fun `parse full order summary with discounts, taxes, and shipping`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_ORDER_SUMMARY_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.amount).isEqualTo(4044L)

        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.subtotal).isEqualTo(5000L)
        assertThat(totalSummary?.totalDueToday).isEqualTo(4044L)
        assertThat(totalSummary?.totalAmountDue).isEqualTo(4044L)

        // Discounts
        assertThat(totalSummary?.discountAmounts).hasSize(2)
        assertThat(totalSummary?.discountAmounts?.get(0)?.amount).isEqualTo(500L)
        assertThat(totalSummary?.discountAmounts?.get(0)?.displayName).isEqualTo("SUMMER10")
        assertThat(totalSummary?.discountAmounts?.get(1)?.amount).isEqualTo(250L)
        assertThat(totalSummary?.discountAmounts?.get(1)?.displayName).isEqualTo("LOYALTY5")

        // Taxes
        assertThat(totalSummary?.taxAmounts).hasSize(1)
        assertThat(totalSummary?.taxAmounts?.get(0)?.amount).isEqualTo(294L)
        assertThat(totalSummary?.taxAmounts?.get(0)?.inclusive).isFalse()
        assertThat(totalSummary?.taxAmounts?.get(0)?.displayName).isEqualTo("Sales Tax")
        assertThat(totalSummary?.taxAmounts?.get(0)?.percentage).isEqualTo(6.875)

        // Shipping
        assertThat(totalSummary?.shippingRate).isNotNull()
        assertThat(totalSummary?.shippingRate?.id).isEqualTo("shr_standard")
        assertThat(totalSummary?.shippingRate?.amount).isEqualTo(500L)
        assertThat(totalSummary?.shippingRate?.displayName).isEqualTo("Standard Shipping")
        assertThat(totalSummary?.shippingRate?.deliveryEstimate).isEqualTo("5-7 business days")

        // No applied balance
        assertThat(totalSummary?.appliedBalance).isNull()
    }

    @Test
    fun `parse applied balance`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_APPLIED_BALANCE_JSON)

        assertThat(result).isNotNull()
        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.subtotal).isEqualTo(1000L)
        assertThat(totalSummary?.totalDueToday).isEqualTo(800L)
        assertThat(totalSummary?.totalAmountDue).isEqualTo(1000L)
        assertThat(totalSummary?.appliedBalance).isEqualTo(-200L)
    }

    @Test
    fun `parse shipping option fallback`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_SHIPPING_OPTION_JSON)

        assertThat(result).isNotNull()
        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.shippingRate).isNotNull()
        assertThat(totalSummary?.shippingRate?.id).isEqualTo("shr_express")
        assertThat(totalSummary?.shippingRate?.amount).isEqualTo(500L)
        assertThat(totalSummary?.shippingRate?.displayName).isEqualTo("Express Shipping")
        assertThat(totalSummary?.shippingRate?.deliveryEstimate).isEqualTo("1-3 business days")
    }

    @Test
    fun `parse without total_summary falls back to line_item_group`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITHOUT_TOTAL_SUMMARY_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.amount).isEqualTo(2000L)

        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.subtotal).isEqualTo(2000L)
        assertThat(totalSummary?.totalDueToday).isEqualTo(2000L)
        assertThat(totalSummary?.totalAmountDue).isEqualTo(2000L)
        assertThat(totalSummary?.discountAmounts).isEmpty()
        assertThat(totalSummary?.taxAmounts).isEmpty()
        assertThat(totalSummary?.shippingRate).isNull()
        assertThat(totalSummary?.appliedBalance).isNull()
    }

    @Test
    fun `parse filters out zero-amount tax entries`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_MIXED_TAX_AMOUNTS_JSON)

        assertThat(result).isNotNull()
        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()

        // Only the non-zero tax amount (Sales Tax at 294) should be included
        assertThat(totalSummary?.taxAmounts).hasSize(1)
        assertThat(totalSummary?.taxAmounts?.get(0)?.amount).isEqualTo(294L)
        assertThat(totalSummary?.taxAmounts?.get(0)?.displayName).isEqualTo("Sales Tax")
        assertThat(totalSummary?.taxAmounts?.get(0)?.percentage).isEqualTo(6.875)
    }

    @Test
    fun `parse with empty discount and tax arrays`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_abc123",
                "currency": "usd",
                "total_summary": {
                    "due": 1000,
                    "subtotal": 1000,
                    "total": 1000
                },
                "line_item_group": {
                    "currency": "usd",
                    "total": 1000,
                    "subtotal": 1000,
                    "due": 1000,
                    "discount_amounts": [],
                    "tax_amounts": []
                },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.discountAmounts).isEmpty()
        assertThat(totalSummary?.taxAmounts).isEmpty()
    }

    @Test
    fun `parse returns null totalSummary when neither total_summary nor line_item_group has subtotal`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000 }
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.totalSummary).isNull()
    }

    @Test
    fun `parse discount amounts from coupon field`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_abc123",
                "currency": "usd",
                "total_summary": {
                    "due": 4099,
                    "subtotal": 5099,
                    "total": 4099
                },
                "line_item_group": {
                    "due": 4099,
                    "subtotal": 5099,
                    "total": 4099,
                    "discount_amounts": [
                        {
                            "amount": 1000,
                            "coupon": {
                                "object": "coupon",
                                "name": "10OFF",
                                "amount_off": 1000,
                                "currency": "usd",
                                "duration": "once"
                            },
                            "currency": "usd",
                            "promotion_code": {
                                "object": "promotion_code",
                                "code": "10OFF"
                            }
                        }
                    ]
                }
            }
            """.trimIndent()
        )
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.amount).isEqualTo(4099L)
        val totalSummary = result?.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary?.discountAmounts).hasSize(1)
        assertThat(totalSummary?.discountAmounts?.get(0)?.amount).isEqualTo(1000L)
        assertThat(totalSummary?.discountAmounts?.get(0)?.displayName).isEqualTo("10OFF")
    }

    @Test
    fun `parse shipping options from root level`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_WITH_SHIPPING_OPTIONS_JSON)

        assertThat(result).isNotNull()
        val shippingOptions = result!!.shippingOptions
        assertThat(shippingOptions).hasSize(3)

        assertThat(shippingOptions[0].id).isEqualTo("shr_standard")
        assertThat(shippingOptions[0].amount).isEqualTo(500L)
        assertThat(shippingOptions[0].displayName).isEqualTo("Standard Shipping")
        assertThat(shippingOptions[0].deliveryEstimate).isNull()

        assertThat(shippingOptions[1].id).isEqualTo("shr_express")
        assertThat(shippingOptions[1].amount).isEqualTo(1500L)
        assertThat(shippingOptions[1].displayName).isEqualTo("Express Shipping")

        assertThat(shippingOptions[2].id).isEqualTo("shr_free")
        assertThat(shippingOptions[2].amount).isEqualTo(0L)
        assertThat(shippingOptions[2].displayName).isEqualTo("Free Shipping")
    }

    @Test
    fun `parse returns empty shipping options when not present`() {
        val result = CheckoutSessionResponseJsonParser(isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_RESPONSE_JSON)

        assertThat(result).isNotNull()
        assertThat(result!!.shippingOptions).isEmpty()
    }
}
