package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
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
    fun `createWithState produces Checkout with correct checkoutSession id`() = runCreateWithStateScenario {
        assertThat(checkoutSessionTurbine.awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `configure returns Checkout with checkoutSession id from network response`() = runConfigureScenario(
        clientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        networkSetup = {
            networkRule.checkoutInit { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
        },
    ) { result ->
        val checkout = result.getOrThrow()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id)
                .isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `applyPromotionCode updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.applyPromotionCode("10OFF")

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        val totalSummary = updated.totalSummary
        assertThat(totalSummary).isNotNull()
        assertThat(totalSummary!!.discountAmounts).hasSize(1)
        assertThat(totalSummary.discountAmounts[0].displayName).isEqualTo("10OFF")
    }

    @Test
    fun `applyPromotionCode returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid promotion code"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.applyPromotionCode("INVALID")
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `applyPromotionCode trims whitespace from promotion code`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.applyPromotionCode("  10OFF  ")

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.totalSummary).isNotNull()
    }

    @Test
    fun `removePromotionCode updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", ""),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.removePromotionCode()

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.totalSummary).isNotNull()
    }

    @Test
    fun `removePromotionCode returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Failed to remove promotion code"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.removePromotionCode()
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `refresh updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.refresh()

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.totalSummary).isNotNull()
    }

    @Test
    fun `refresh returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutInit { response ->
            response.setResponseCode(500)
            response.setBody("""{"error": {"message": "Internal server error"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.refresh()
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `updateLineItemQuantity updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart(urlEncode("updated_line_item_quantity[line_item_id]"), "li_1"),
            bodyPart(urlEncode("updated_line_item_quantity[quantity]"), "3"),
            bodyPart(urlEncode("updated_line_item_quantity[fail_update_on_discount_error]"), "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-update-quantity.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().lineItems).isEmpty()

        val result = checkout.updateLineItemQuantity("li_1", 3)

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.lineItems).hasSize(1)
        assertThat(updated.lineItems[0].id).isEqualTo("li_1")
        assertThat(updated.lineItems[0].quantity).isEqualTo(3)
    }

    @Test
    fun `updateLineItemQuantity returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid quantity"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.updateLineItemQuantity("li_1", -1)
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `selectShippingOption updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart("shipping_rate", "shr_express"),
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-select-shipping-rate.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().shippingOptions).isEmpty()

        val result = checkout.selectShippingOption("shr_express")

        val updated = checkoutSessionTurbine.awaitItem()
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

    @Test
    fun `updateShippingAddress sends address fields and updates checkoutSession on success`() =
        runCreateWithStateScenario {
            networkRule.checkoutUpdate(
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[city]"), "Denver"),
                bodyPart(urlEncode("tax_region[state]"), "CO"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            val initial = checkoutSessionTurbine.awaitItem()
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

            val updated = checkoutSessionTurbine.awaitItem()
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

    @Test
    fun `updateShippingAddress returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid address"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val address = Address()
            .country("XX")
        val result = checkout.updateShippingAddress(address = address)
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `updateShippingAddress omits empty fields from request`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
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

        checkoutSessionTurbine.awaitItem()

        val address = Address()
            .country("US")
            .postalCode("80202")
        val result = checkout.updateShippingAddress(address = address)

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(updated)
    }

    @Test
    fun `updateBillingAddress sends address fields and updates checkoutSession on success`() =
        runCreateWithStateScenario {
            networkRule.checkoutUpdate(
                bodyPart(urlEncode("tax_region[country]"), "US"),
                bodyPart(urlEncode("tax_region[city]"), "Denver"),
                bodyPart(urlEncode("tax_region[state]"), "CO"),
                bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
                bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-update-shipping-address.json")
            }

            val initial = checkoutSessionTurbine.awaitItem()
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

            val updated = checkoutSessionTurbine.awaitItem()
            assertThat(result.getOrThrow()).isEqualTo(updated)
            assertThat(updated.shippingOptions).hasSize(3)
            val totalSummary = updated.totalSummary
            assertThat(totalSummary).isNotNull()
            val shippingRate = requireNotNull(totalSummary!!.shippingRate)
            assertThat(shippingRate.id).isEqualTo("shr_1T95aTLu5o3P18ZpgwZ8uTrs")
            assertThat(shippingRate.displayName).isEqualTo("Free Shipping")
            assertThat(totalSummary.taxAmounts).hasSize(2)
        }

    @Test
    fun `updateBillingAddress returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid address"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val address = Address()
            .country("XX")
        val result = checkout.updateBillingAddress(address = address)
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `updateShippingAddress stores address in internalState`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
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

    @Test
    fun `updateBillingAddress stores address in internalState`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
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

    @Test
    fun `updateShippingAddress does not store address in internalState on failure`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
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

    @Test
    fun `updateBillingAddress does not store address in internalState on failure`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
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

    @Test
    fun `updateShippingAddress stores phoneNumber in internalState`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-update-shipping-address.json")
        }

        val address = Address().country("US")
        val result = checkout.updateShippingAddress(phoneNumber = "5551234567", address = address)
        assertThat(result.isSuccess).isTrue()

        assertThat(checkout.internalState.shippingPhoneNumber).isEqualTo("5551234567")
    }

    @Test
    fun `updateBillingAddress stores phoneNumber in internalState`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-update-shipping-address.json")
        }

        val address = Address().country("US")
        val result = checkout.updateBillingAddress(phoneNumber = "5559876543", address = address)
        assertThat(result.isSuccess).isTrue()

        assertThat(checkout.internalState.billingPhoneNumber).isEqualTo("5559876543")
    }

    @Test
    fun `updateShippingAddress does not store phoneNumber in internalState on failure`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid address"}}""")
        }

        val address = Address().country("XX")
        val result = checkout.updateShippingAddress(phoneNumber = "5551234567", address = address)
        assertThat(result.isFailure).isTrue()

        assertThat(checkout.internalState.shippingPhoneNumber).isNull()
    }

    @Test
    fun `updateBillingAddress does not store phoneNumber in internalState on failure`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid address"}}""")
        }

        val address = Address().country("XX")
        val result = checkout.updateBillingAddress(phoneNumber = "5559876543", address = address)
        assertThat(result.isFailure).isTrue()

        assertThat(checkout.internalState.billingPhoneNumber).isNull()
    }

    @Test
    fun `updateBillingAddress omits empty fields from request`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
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

        checkoutSessionTurbine.awaitItem()

        val address = Address()
            .country("US")
            .postalCode("80202")
        val result = checkout.updateBillingAddress(address = address)

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(updated)
    }

    @Test
    fun `updateTaxId sends type and value and updates checkoutSession on success`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart(urlEncode("tax_id_collection[tax_id][type]"), "us_ein"),
            bodyPart(urlEncode("tax_id_collection[tax_id][value]"), "123456789"),
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.updateTaxId("us_ein", "123456789")

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.totalSummary).isNotNull()
    }

    @Test
    fun `updateTaxId returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid tax ID"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.updateTaxId("invalid", "000")
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `updateTaxId trims whitespace from type and value`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate(
            bodyPart(urlEncode("tax_id_collection[tax_id][type]"), "us_ein"),
            bodyPart(urlEncode("tax_id_collection[tax_id][value]"), "123456789"),
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        assertThat(checkoutSessionTurbine.awaitItem().totalSummary).isNull()

        val result = checkout.updateTaxId("  us_ein  ", "  123456789  ")

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(result.getOrThrow()).isEqualTo(updated)
        assertThat(updated.totalSummary).isNotNull()
    }

    @Test
    fun `selectShippingOption returns failure on error response`() = runCreateWithStateScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid shipping rate"}}""")
        }

        val initial = checkoutSessionTurbine.awaitItem()

        val result = checkout.selectShippingOption("shr_invalid")
        assertThat(result.isFailure).isTrue()

        checkoutSessionTurbine.expectNoEvents()
        assertThat(checkout.checkoutSession.value).isEqualTo(initial)
    }

    @Test
    fun `concurrent calls to withInternalState are serialized`() = runCreateWithStateScenario {
        // First call: applyPromotionCode hits the initial session ID with promotion_code param.
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
        ) { response ->
            response.setBodyDelay(200, TimeUnit.MILLISECONDS)
            response.testBodyFromFile("checkout-session-concurrent-apply-promo.json")
        }

        // Second call: updateShippingAddress must use the session ID from the first response,
        // proving the mutex serialized the calls.
        networkRule.checkoutUpdate(
            bodyPart(urlEncode("tax_region[country]"), "US"),
            bodyPart(urlEncode("tax_region[postal_code]"), "80202"),
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            sessionId = "cs_test_after_promo",
        ) { response ->
            response.testBodyFromFile("checkout-session-concurrent-update-address.json")
        }

        val initial = checkoutSessionTurbine.awaitItem()
        assertThat(initial.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
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

        val afterPromo = checkoutSessionTurbine.awaitItem()
        assertThat(afterPromo.id).isEqualTo("cs_test_after_promo")
        val promoSummary = requireNotNull(afterPromo.totalSummary)
        assertThat(promoSummary.totalDueToday).isEqualTo(4099)
        assertThat(promoSummary.discountAmounts).hasSize(1)
        assertThat(promoSummary.discountAmounts[0].displayName).isEqualTo("10OFF")

        val afterAddress = checkoutSessionTurbine.awaitItem()
        assertThat(afterAddress.id).isEqualTo("cs_test_after_address")
        val addressSummary = requireNotNull(afterAddress.totalSummary)
        assertThat(addressSummary.totalDueToday).isEqualTo(4599)
        val shippingRate = requireNotNull(addressSummary.shippingRate)
        assertThat(shippingRate.id).isEqualTo("shr_express")
        assertThat(shippingRate.amount).isEqualTo(500)
        assertThat(shippingRate.displayName).isEqualTo("Express Shipping")
    }

    @Test
    fun `updateWithResponse updates checkoutSession flow`() = runCreateWithStateScenario {
        val initial = checkoutSessionTurbine.awaitItem()
        assertThat(initial.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
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

        val updated = checkoutSessionTurbine.awaitItem()
        assertThat(updated.id).isEqualTo("cs_test_updated")
        assertThat(updated.totalSummary).isNotNull()
        assertThat(updated.totalSummary!!.subtotal).isEqualTo(2000L)
    }

    @Test
    fun `updateWithResponse updates internalState`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

        val updatedResponse = CheckoutSessionResponseFactory.create(id = "cs_test_updated")
        checkout.updateWithResponse(updatedResponse)

        assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo("cs_test_updated")
        assertThat(checkout.internalState.checkoutSessionResponse).isEqualTo(updatedResponse)
    }

    @Test
    fun `updateWithResponse preserves non-response internalState fields`() = runTest {
        val initialResponse = CheckoutSessionResponseFactory.create()
        val state = CheckoutStateFactory.create(
            configuration = Checkout.Configuration().adaptivePricingAllowed(true).build(),
            checkoutSessionResponse = initialResponse,
            shippingName = "Jane Doe",
            billingName = "John Doe",
        )
        val checkout = Checkout.createWithState(applicationContext, state)

        val updatedResponse = CheckoutSessionResponseFactory.create(id = "cs_test_updated")
        checkout.updateWithResponse(updatedResponse)

        assertThat(checkout.internalState.shippingName).isEqualTo("Jane Doe")
        assertThat(checkout.internalState.billingName).isEqualTo("John Doe")
        assertThat(checkout.internalState.checkoutSessionResponse.id).isEqualTo("cs_test_updated")
        assertThat(checkout.internalState.configuration.adaptivePricingAllowed).isTrue()
    }

    @Test
    fun `ensureNoMutationInFlight throws when mutex is locked`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.setBodyDelay(5, TimeUnit.SECONDS)
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        // Start a mutation to lock the mutex.
        val deferred = async { checkout.applyPromotionCode("10OFF") }

        // Give the coroutine time to acquire the mutex.
        testScheduler.advanceUntilIdle()

        val error = runCatching { checkout.ensureNoMutationInFlight() }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat()
            .isEqualTo("Cannot launch while a checkout session mutation is in flight.")

        deferred.cancel()
    }

    @Test
    fun `setting state throws when mutex is locked`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        networkRule.checkoutUpdate { response ->
            response.setBodyDelay(5, TimeUnit.SECONDS)
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        // Start a mutation to lock the mutex.
        val deferred = async { checkout.applyPromotionCode("10OFF") }

        // Give the coroutine time to acquire the mutex.
        testScheduler.advanceUntilIdle()

        val error = runCatching { checkout.state = checkout.state }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat()
            .isEqualTo("Cannot launch while a checkout session mutation is in flight.")

        deferred.cancel()
    }

    @Test
    fun `mutation returns failure when integrationLaunched is true`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        checkout.markIntegrationLaunched()

        val result = checkout.applyPromotionCode("10OFF")
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `mutation succeeds after markIntegrationDismissed`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        checkout.markIntegrationLaunched()
        checkout.markIntegrationDismissed()

        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        val result = checkout.applyPromotionCode("10OFF")
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `integrationLaunched persists through State restoration`() = runCreateWithStateScenario(
        shouldValidateEvents = false,
    ) {
        checkout.markIntegrationLaunched()

        val restoredCheckout = Checkout.createWithState(applicationContext, checkout.state)

        val result = restoredCheckout.applyPromotionCode("10OFF")
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `configure sends adaptive_pricing allowed false by default`() = runConfigureScenario(
        clientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        networkSetup = {
            networkRule.checkoutInit(
                bodyPart(urlEncode("adaptive_pricing[allowed]"), "false"),
            ) { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
        },
    ) { result ->
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure sends adaptive_pricing allowed true when configured`() = runConfigureScenario(
        clientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        configuration = Checkout.Configuration().adaptivePricingAllowed(true),
        networkSetup = {
            networkRule.checkoutInit(
                bodyPart(urlEncode("adaptive_pricing[allowed]"), "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
        },
    ) { result ->
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure returns failure when network request fails`() = runConfigureScenario(
        clientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_xyz",
        networkSetup = {
            networkRule.checkoutInit { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) { result ->
        assertThat(result.isFailure).isTrue()
    }

    private fun runCreateWithStateScenario(
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        shouldValidateEvents: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val state = CheckoutStateFactory.create(
            key = "CheckoutTest",
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val checkout = Checkout.createWithState(applicationContext, state)
        val bgScope = backgroundScope
        turbineScope {
            val checkoutSessionTurbine = checkout.checkoutSession.testIn(bgScope)
            block(
                Scenario(
                    checkout = checkout,
                    testScope = this@runTest,
                    checkoutSessionTurbine = checkoutSessionTurbine,
                )
            )
            if (shouldValidateEvents) {
                checkoutSessionTurbine.ensureAllEventsConsumed()
            } else {
                checkoutSessionTurbine.cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun runConfigureScenario(
        clientSecret: String,
        configuration: Checkout.Configuration = Checkout.Configuration(),
        networkSetup: () -> Unit,
        block: suspend (Result<Checkout>) -> Unit,
    ) = runTest {
        networkSetup()
        val result = Checkout.configure(
            context = applicationContext,
            checkoutSessionClientSecret = clientSecret,
            configuration = configuration,
        )
        block(result)
    }

    private class Scenario(
        val checkout: Checkout,
        private val testScope: TestScope,
        val checkoutSessionTurbine: ReceiveTurbine<CheckoutSession>,
    ) : CoroutineScope by testScope {
        val testScheduler: TestCoroutineScheduler get() = testScope.testScheduler
    }
}
