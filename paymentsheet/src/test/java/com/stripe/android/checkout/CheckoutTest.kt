package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `createWithState produces Checkout with correct checkoutSession id`() = runTest {
        runCreateWithStateScenario { checkout ->
            checkout.checkoutSession.test {
                assertThat(awaitItem().id).isEqualTo("cs_test_abc123")
            }
        }
    }

    @Test
    fun `configure returns Checkout with checkoutSession id from network response`() = runConfigureScenario(
        clientSecret = "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh_secret_example",
        networkSetup = {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh/init"),
            ) { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
        },
    ) { result ->
        val checkout = result.getOrThrow()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id)
                .isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
        }
    }

    @Test
    fun `applyPromotionCode updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.applyPromotionCode("10OFF")

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                val totalSummary = updated.totalSummary
                assertThat(totalSummary).isNotNull()
                assertThat(totalSummary!!.discountAmounts).hasSize(1)
                assertThat(totalSummary.discountAmounts[0].displayName).isEqualTo("10OFF")
            }
        }
    }

    @Test
    fun `applyPromotionCode returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid promotion code"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.applyPromotionCode("INVALID")
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `applyPromotionCode trims whitespace from promotion code`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.applyPromotionCode("  10OFF  ")

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.totalSummary).isNotNull()
            }
        }
    }

    @Test
    fun `removePromotionCode updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", ""),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.removePromotionCode()

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.totalSummary).isNotNull()
            }
        }
    }

    @Test
    fun `removePromotionCode returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Failed to remove promotion code"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.removePromotionCode()
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `refresh updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123/init"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.refresh()

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.totalSummary).isNotNull()
            }
        }
    }

    @Test
    fun `refresh returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123/init"),
            ) { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.refresh()
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `updateLineItemQuantity updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("updated_line_item_quantity[line_item_id]"), "li_1"),
                bodyPart(urlEncode("updated_line_item_quantity[quantity]"), "3"),
                bodyPart(urlEncode("updated_line_item_quantity[fail_update_on_discount_error]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-quantity.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().lineItems).isEmpty()

                val result = checkout.updateLineItemQuantity("li_1", 3)

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.lineItems).hasSize(1)
                assertThat(updated.lineItems[0].id).isEqualTo("li_1")
                assertThat(updated.lineItems[0].quantity).isEqualTo(3)
            }
        }
    }

    @Test
    fun `updateLineItemQuantity returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid quantity"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.updateLineItemQuantity("li_1", -1)
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `selectShippingRate updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("shipping_rate", "shr_express"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-select-shipping-rate.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().shippingOptions).isEmpty()

                val result = checkout.selectShippingRate("shr_express")

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                val totalSummary = updated.totalSummary
                assertThat(totalSummary).isNotNull()
                val shippingRate = requireNotNull(totalSummary!!.shippingRate)
                assertThat(shippingRate).isNotNull()
                assertThat(shippingRate.id).isEqualTo("shr_express")
                assertThat(shippingRate.amount).isEqualTo(1500L)
                assertThat(shippingRate.displayName).isEqualTo("Express Shipping")
                assertThat(updated.shippingOptions).hasSize(2)
                assertThat(updated.shippingOptions[0].id).isEqualTo("shr_standard")
                assertThat(updated.shippingOptions[1].id).isEqualTo("shr_express")
            }
        }
    }

    @Test
    fun `updateShippingAddress sends address fields and updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[city]"), "Denver"),
                bodyPart(urlEncode("tax_region[state]"), "CO"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()
                assertThat(initial.shippingOptions).isEmpty()
                assertThat(initial.totalSummary?.taxAmounts).isNull()

                val address = Address()
                    .city("Denver")
                    .country("US")
                    .line1("123 Main St")
                    .line2("Apt 4")
                    .postalCode("80202")
                    .state("CO")
                val result = checkout.updateShippingAddress(address = address)

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.shippingOptions).hasSize(3)
                val totalSummary = updated.totalSummary
                assertThat(totalSummary).isNotNull()
                val shippingRate = requireNotNull(totalSummary!!.shippingRate)
                assertThat(shippingRate.id).isEqualTo("shr_1T95aTLu5o3P18ZpgwZ8uTrs")
                assertThat(shippingRate.displayName).isEqualTo("Free Shipping")
                assertThat(totalSummary.taxAmounts).hasSize(2)
                assertThat(totalSummary.taxAmounts[0].amount).isEqualTo(0)
                assertThat(totalSummary.taxAmounts[0].inclusive).isFalse()
                assertThat(totalSummary.taxAmounts[0].displayName).isEqualTo("Sales Tax")
                assertThat(totalSummary.taxAmounts[0].percentage).isEqualTo(8.2)
                assertThat(totalSummary.taxAmounts[1].amount).isEqualTo(0)
                assertThat(totalSummary.taxAmounts[1].inclusive).isFalse()
                assertThat(totalSummary.taxAmounts[1].displayName).isEqualTo("Retail Delivery Fee")
                assertThat(totalSummary.taxAmounts[1].percentage).isEqualTo(0.0)
                assertThat(updated.lineItems).hasSize(1)
                assertThat(updated.lineItems[0].name).isEqualTo("Llama Figure")
            }
        }
    }

    @Test
    fun `updateShippingAddress returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val address = Address()
                    .country("XX")
                val result = checkout.updateShippingAddress(address = address)
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `updateShippingAddress omits empty fields from request`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                not(hasBodyPart(urlEncode("tax_region[city]"))),
                not(hasBodyPart(urlEncode("tax_region[state]"))),
                not(hasBodyPart(urlEncode("tax_region[line1]"))),
                not(hasBodyPart(urlEncode("tax_region[line2]"))),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            checkout.checkoutSession.test {
                awaitItem()

                val address = Address()
                    .country("US")
                    .postalCode("80202")
                val result = checkout.updateShippingAddress(address = address)

                val updated = awaitItem()
                assertThat(result.isSuccess).isTrue()
                assertThat(result.getOrThrow()).isEqualTo(updated)
            }
        }
    }

    @Test
    fun `updateBillingAddress sends address fields and updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[city]"), "Denver"),
                bodyPart(urlEncode("tax_region[state]"), "CO"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()
                assertThat(initial.shippingOptions).isEmpty()
                assertThat(initial.totalSummary?.taxAmounts).isNull()

                val address = Address()
                    .city("Denver")
                    .country("US")
                    .line1("123 Main St")
                    .line2("Apt 4")
                    .postalCode("80202")
                    .state("CO")
                val result = checkout.updateBillingAddress(address = address)

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.shippingOptions).hasSize(3)
                val totalSummary = updated.totalSummary
                assertThat(totalSummary).isNotNull()
                val shippingRate = requireNotNull(totalSummary!!.shippingRate)
                assertThat(shippingRate.id).isEqualTo("shr_1T95aTLu5o3P18ZpgwZ8uTrs")
                assertThat(shippingRate.displayName).isEqualTo("Free Shipping")
                assertThat(totalSummary.taxAmounts).hasSize(2)
            }
        }
    }

    @Test
    fun `updateBillingAddress returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val address = Address()
                    .country("XX")
                val result = checkout.updateBillingAddress(address = address)
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `updateShippingAddress stores address in internalState`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            val address = Address()
                .city("Denver")
                .country("US")
                .line1("123 Main St")
                .line2("Apt 4")
                .postalCode("80202")
                .state("CO")
            val result = checkout.updateShippingAddress(name = "John", address = address)
            assertThat(result.isSuccess).isTrue()

            val state = checkout.internalState
            assertThat(state.shippingName).isEqualTo("John")
            assertThat(state.shippingAddress).isEqualTo(
                Address.State(
                    city = "Denver",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    postalCode = "80202",
                    state = "CO",
                )
            )
        }
    }

    @Test
    fun `updateBillingAddress stores address in internalState`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            val address = Address()
                .city("Denver")
                .country("US")
                .line1("123 Main St")
                .line2("Apt 4")
                .postalCode("80202")
                .state("CO")
            val result = checkout.updateBillingAddress(name = "Jane", address = address)
            assertThat(result.isSuccess).isTrue()

            val state = checkout.internalState
            assertThat(state.billingName).isEqualTo("Jane")
            assertThat(state.billingAddress).isEqualTo(
                Address.State(
                    city = "Denver",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    postalCode = "80202",
                    state = "CO",
                )
            )
        }
    }

    @Test
    fun `updateShippingAddress does not store address in internalState on failure`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            val address = Address()
                .country("XX")
            val result = checkout.updateShippingAddress(name = "John", address = address)
            assertThat(result.isFailure).isTrue()

            assertThat(checkout.internalState.shippingName).isNull()
            assertThat(checkout.internalState.shippingAddress).isNull()
        }
    }

    @Test
    fun `updateBillingAddress does not store address in internalState on failure`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            val address = Address()
                .country("XX")
            val result = checkout.updateBillingAddress(name = "Jane", address = address)
            assertThat(result.isFailure).isTrue()

            assertThat(checkout.internalState.billingName).isNull()
            assertThat(checkout.internalState.billingAddress).isNull()
        }
    }

    @Test
    fun `updateBillingAddress omits empty fields from request`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                not(hasBodyPart(urlEncode("tax_region[city]"))),
                not(hasBodyPart(urlEncode("tax_region[state]"))),
                not(hasBodyPart(urlEncode("tax_region[line1]"))),
                not(hasBodyPart(urlEncode("tax_region[line2]"))),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            checkout.checkoutSession.test {
                awaitItem()

                val address = Address()
                    .country("US")
                    .postalCode("80202")
                val result = checkout.updateBillingAddress(address = address)

                val updated = awaitItem()
                assertThat(result.isSuccess).isTrue()
                assertThat(result.getOrThrow()).isEqualTo(updated)
            }
        }
    }

    @Test
    fun `updateTaxId sends type and value and updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_id_collection[tax_id][type]"), "us_ein"),
                bodyPart(urlEncode("tax_id_collection[tax_id][value]"), "123456789"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.updateTaxId("us_ein", "123456789")

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.totalSummary).isNotNull()
            }
        }
    }

    @Test
    fun `updateTaxId returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid tax ID"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.updateTaxId("invalid", "000")
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `updateTaxId trims whitespace from type and value`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart(urlEncode("tax_id_collection[tax_id][type]"), "us_ein"),
                bodyPart(urlEncode("tax_id_collection[tax_id][value]"), "123456789"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            checkout.checkoutSession.test {
                assertThat(awaitItem().totalSummary).isNull()

                val result = checkout.updateTaxId("  us_ein  ", "  123456789  ")

                val updated = awaitItem()
                assertThat(result.getOrThrow()).isEqualTo(updated)
                assertThat(updated.totalSummary).isNotNull()
            }
        }
    }

    @Test
    fun `selectShippingRate returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid shipping rate"}}""")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()

                val result = checkout.selectShippingRate("shr_invalid")
                assertThat(result.isFailure).isTrue()

                expectNoEvents()
                assertThat(checkout.checkoutSession.value).isEqualTo(initial)
            }
        }
    }

    @Test
    fun `concurrent calls to withSessionId are serialized`() = runTest {
        runCreateWithStateScenario { checkout ->
            // First call: applyPromotionCode hits the initial session ID with promotion_code param.
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.setBodyDelay(200, TimeUnit.MILLISECONDS)
                response.testBodyFromFile("checkout-session-concurrent-apply-promo.json")
            }

            // Second call: updateShippingAddress must use the session ID from the first response,
            // proving the mutex serialized the calls.
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_after_promo"),
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-concurrent-update-address.json")
            }

            checkout.checkoutSession.test {
                val initial = awaitItem()
                assertThat(initial.id).isEqualTo("cs_test_abc123")
                assertThat(initial.totalSummary).isNull()

                val address = Address()
                    .country("US")
                    .postalCode("80202")

                val results = listOf(
                    async { checkout.applyPromotionCode("10OFF") },
                    async { checkout.updateShippingAddress(address = address) },
                ).awaitAll()

                assertThat(results[0].isSuccess).isTrue()
                assertThat(results[1].isSuccess).isTrue()

                val afterPromo = awaitItem()
                assertThat(afterPromo.id).isEqualTo("cs_test_after_promo")
                val promoSummary = requireNotNull(afterPromo.totalSummary)
                assertThat(promoSummary.totalDueToday).isEqualTo(4099)
                assertThat(promoSummary.discountAmounts).hasSize(1)
                assertThat(promoSummary.discountAmounts[0].displayName).isEqualTo("10OFF")

                val afterAddress = awaitItem()
                assertThat(afterAddress.id).isEqualTo("cs_test_after_address")
                val addressSummary = requireNotNull(afterAddress.totalSummary)
                assertThat(addressSummary.totalDueToday).isEqualTo(4599)
                val shippingRate = requireNotNull(addressSummary.shippingRate)
                assertThat(shippingRate.id).isEqualTo("shr_express")
                assertThat(shippingRate.amount).isEqualTo(500)
                assertThat(shippingRate.displayName).isEqualTo("Express Shipping")
            }
        }
    }

    @Test
    fun `updateWithResponse updates checkoutSession flow`() = runTest {
        runCreateWithStateScenario { checkout ->
            checkout.checkoutSession.test {
                val initial = awaitItem()
                assertThat(initial.id).isEqualTo("cs_test_abc123")
                assertThat(initial.totalSummary).isNull()

                val updatedResponse = CheckoutSessionResponseFactory.create(
                    id = "cs_test_updated",
                    totalSummary = CheckoutSessionResponse.TotalSummaryResponse(
                        subtotal = 2000L,
                        totalDueToday = 2000L,
                        totalAmountDue = 2000L,
                        discountAmounts = emptyList(),
                        taxAmounts = emptyList(),
                        shippingRate = null,
                        appliedBalance = null,
                    ),
                )
                checkout.updateWithResponse(updatedResponse)

                val updated = awaitItem()
                assertThat(updated.id).isEqualTo("cs_test_updated")
                assertThat(updated.totalSummary).isNotNull()
                assertThat(updated.totalSummary!!.subtotal).isEqualTo(2000L)
            }
        }
    }

    @Test
    fun `updateWithResponse updates internalState`() = runTest {
        runCreateWithStateScenario { checkout ->
            assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo("cs_test_abc123")

            val updatedResponse = CheckoutSessionResponseFactory.create(id = "cs_test_updated")
            checkout.updateWithResponse(updatedResponse)

            assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo("cs_test_updated")
            assertThat(checkout.internalState.checkoutSessionResponse).isEqualTo(updatedResponse)
        }
    }

    @Test
    fun `updateWithResponse preserves non-response internalState fields`() = runTest {
        val initialResponse = CheckoutSessionResponseFactory.create()
        val state = Checkout.State(
            InternalState(
                key = "CheckoutTest",
                checkoutSessionResponse = initialResponse,
                shippingName = "Jane Doe",
                billingName = "John Doe",
            ),
        )
        val checkout = Checkout.createWithState(applicationContext, state)

        val updatedResponse = CheckoutSessionResponseFactory.create(id = "cs_test_updated")
        checkout.updateWithResponse(updatedResponse)

        assertThat(checkout.internalState.shippingName).isEqualTo("Jane Doe")
        assertThat(checkout.internalState.billingName).isEqualTo("John Doe")
        assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo("cs_test_updated")
    }

    @Test
    fun `configure returns failure when network request fails`() = runConfigureScenario(
        clientSecret = "cs_test_abc123_secret_xyz",
        networkSetup = {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123/init"),
            ) { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) { result ->
        assertThat(result.isFailure).isTrue()
    }

    private suspend fun runCreateWithStateScenario(
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        block: suspend (Checkout) -> Unit,
    ) {
        val state = Checkout.State(
            InternalState(
                key = "CheckoutTest",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        )
        val checkout = Checkout.createWithState(applicationContext, state)
        block(checkout)
    }

    private fun runConfigureScenario(
        clientSecret: String,
        networkSetup: () -> Unit,
        block: suspend (Result<Checkout>) -> Unit,
    ) = runTest {
        networkSetup()
        val result = Checkout.configure(
            context = applicationContext,
            checkoutSessionClientSecret = clientSecret,
        )
        block(result)
    }
}
