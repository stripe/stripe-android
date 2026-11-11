package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
import com.stripe.android.createPaymentMethod
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.paymentelementtestpages.ManagePage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutCardIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contentPage = EmbeddedContentPage(composeRule)
    private val formPage = EmbeddedFormPage(composeRule)
    private val managePage = ManagePage(composeRule)

    private val configuration = CheckoutElementsController.Configuration()
        .merchantDisplayName("Checkout E2E")
        .paymentElement(
            PaymentElement.Configuration()
                .googlePayConfiguration(
                    PaymentElement.Configuration.GooglePayConfiguration()
                        .display(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
                )
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration()
                        .display(PaymentElement.Configuration.LinkConfiguration.Display.Never)
                )
        )

    @Test
    fun testSuccessfulCardPayment() = runScenario(
        setup = {
            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails(newCardNumber = "4242424242424242")
            formPage.clickPrimaryButton()
        },
        check = { result ->
            assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
        },
    )

    @Test
    fun testDeclinedCardReportsFailure() = runScenario(
        setup = {
            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails(newCardNumber = "4000000000000002")
            formPage.clickPrimaryButton()
        },
        check = { result ->
            assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
            assertThat((result as CheckoutElementsController.Result.Failed).displayMessage)
                .contains("Your card was declined")
        },
    )

    @Test
    fun testSwitchingSavedCardsUpdatesBillingTaxAndCompletesPayment() = runBlocking {
        lateinit var californiaCardId: String
        lateinit var oregonCardId: String

        val result = runCheckoutIntegrationTest(
            merchant = TAX_MERCHANT,
            sessionParams = { backend, stripe ->
                val customerId = backend.createCustomer(
                    buildJsonObject { put("email", CUSTOMER_EMAIL) },
                ).getOrThrow()
                californiaCardId = createSavedCard(stripe, backend, customerId, CALIFORNIA_ADDRESS, "4242424242424242")
                oregonCardId = createSavedCard(stripe, backend, customerId, OREGON_ADDRESS, "5555555555554444")
                automaticTaxSessionParams(customerId)
            },
            configuration = configuration,
            setup = { controller ->
                val californiaSession = selectSavedCard(controller, californiaCardId, CALIFORNIA_ADDRESS)
                val californiaTotals = californiaSession.totals
                assertThat(californiaTotals.subtotal.minorUnitsAmount).isEqualTo(SUBTOTAL)
                assertThat(californiaTotals.taxExclusive.minorUnitsAmount).isGreaterThan(0L)
                assertThat(californiaTotals.total.minorUnitsAmount).isEqualTo(
                    californiaTotals.subtotal.minorUnitsAmount + californiaTotals.taxExclusive.minorUnitsAmount,
                )

                val oregonSession = selectSavedCard(controller, oregonCardId, OREGON_ADDRESS)
                assertThat(oregonSession.totals.subtotal.minorUnitsAmount).isEqualTo(SUBTOTAL)
                assertThat(oregonSession.totals.taxExclusive.minorUnitsAmount).isEqualTo(0L)
                assertThat(oregonSession.totals.total.minorUnitsAmount).isEqualTo(SUBTOTAL)
            },
        )

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    private fun runScenario(
        setup: suspend (CheckoutElementsController) -> Unit,
        check: (CheckoutElementsController.Result) -> Unit,
    ) = runBlocking {
        val result = runCheckoutIntegrationTest(
            merchant = "US",
            sessionParams = { _, _ -> cardOnlySessionParams() },
            configuration = configuration,
            setup = setup,
        )
        check(result)
    }

    private fun cardOnlySessionParams(): JsonObject {
        val lineItem = buildJsonObject {
            put("quantity", 1)
            put(
                "price_data",
                buildJsonObject {
                    put("currency", "usd")
                    put("unit_amount", 1000)
                    put("product_data", buildJsonObject { put("name", "Checkout E2E") })
                },
            )
        }
        return buildJsonObject {
            put("ui_mode", "mobile_elements")
            put("currency", "usd")
            put("customer_email", "checkout-e2e@example.com")
            put("allowed_payment_method_types", JsonArray(listOf(JsonPrimitive("card"))))
            put(
                "items",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "one_time_price")
                            put("one_time_price", buildJsonObject { put("items", JsonArray(listOf(lineItem))) })
                        },
                    )
                },
            )
        }
    }

    private suspend fun createSavedCard(
        stripe: Stripe,
        backend: CheckoutPlaygroundBackend,
        customerId: String,
        address: Address,
        cardNumber: String,
    ): String {
        val paymentMethod = stripe.createPaymentMethod(
            PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card.Builder()
                    .setNumber(cardNumber)
                    .setExpiryMonth(12)
                    .setExpiryYear(2034)
                    .setCvc("123")
                    .build(),
                billingDetails = PaymentMethod.BillingDetails(
                    address = address,
                    email = CUSTOMER_EMAIL,
                    name = "Jenny Rosen",
                ),
                metadata = null,
                allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
            ),
        )
        backend.attachPaymentMethod(paymentMethod.id, customerId).getOrThrow()
        return paymentMethod.id
    }

    private suspend fun selectSavedCard(
        controller: CheckoutElementsController,
        paymentMethodId: String,
        address: Address,
    ): CheckoutElementsController.Session {
        contentPage.clickViewMore()
        managePage.waitUntilVisible()
        managePage.selectPaymentMethod(paymentMethodId)

        val session = withTimeout(30.seconds) {
            combine(controller.session, controller.isUpdating) { session, isUpdating ->
                session.takeIf {
                    !isUpdating && session?.paymentOption?.billingDetails?.address?.state == address.state
                }
            }.first { it != null }
        }
        managePage.waitUntilNotVisible()
        contentPage.assertHasSelectedSavedPaymentMethod(paymentMethodId)

        val selectedSession = checkNotNull(session)
        assertThat(selectedSession.tax?.status).isEqualTo(CheckoutElementsController.Session.Tax.Status.Ready)
        val selectedAddress = selectedSession.paymentOption?.billingDetails?.address
        assertThat(selectedAddress?.line1).isEqualTo(address.line1)
        assertThat(selectedAddress?.city).isEqualTo(address.city)
        assertThat(selectedAddress?.state).isEqualTo(address.state)
        assertThat(selectedAddress?.postalCode).isEqualTo(address.postalCode)
        assertThat(selectedAddress?.country).isEqualTo(address.country)
        return selectedSession
    }

    private fun automaticTaxSessionParams(customerId: String): JsonObject {
        val lineItem = buildJsonObject {
            put("quantity", 1)
            put(
                "price_data",
                buildJsonObject {
                    put("currency", "usd")
                    put("unit_amount", SUBTOTAL)
                    put("tax_behavior", "exclusive")
                    put(
                        "product_data",
                        buildJsonObject {
                            put("name", "Checkout E2E")
                            put("tax_code", "txcd_99999999")
                        },
                    )
                },
            )
        }
        return buildJsonObject {
            put("ui_mode", "mobile_elements")
            put("currency", "usd")
            put("customer", customerId)
            put("automatic_tax", buildJsonObject { put("enabled", true) })
            put("billing_address_collection", "required")
            put("customer_update", buildJsonObject { put("address", "auto") })
            put("allowed_payment_method_types", JsonArray(listOf(JsonPrimitive("card"))))
            put(
                "saved_payment_method_options",
                buildJsonObject {
                    put("payment_method_save", "disabled")
                    put("payment_method_remove", "disabled")
                },
            )
            put(
                "items",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "one_time_price")
                            put("one_time_price", buildJsonObject { put("items", JsonArray(listOf(lineItem))) })
                        },
                    )
                },
            )
        }
    }

    private companion object {
        const val TAX_MERCHANT = "us_tax"
        const val CUSTOMER_EMAIL = "checkout-e2e@example.com"
        const val SUBTOTAL = 1000L

        val CALIFORNIA_ADDRESS = Address(
            line1 = "123 Main St",
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US",
        )
        val OREGON_ADDRESS = Address(
            line1 = "123 Main St",
            city = "Portland",
            state = "OR",
            postalCode = "97201",
            country = "US",
        )
    }
}
