package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.paymentelementtestpages.BillingDetailsPage
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
    private val billingDetailsPage = BillingDetailsPage(composeRule)
    private val paymentSheetPage = PaymentSheetPage(composeRule)

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
    fun testSavedCardIsReusedWithBillingTaxInAnotherSession() = runBlocking {
        lateinit var customerId: String

        val saveResult = runCheckoutIntegrationTest(
            merchant = TAX_MERCHANT,
            sessionParams = { backend ->
                customerId = backend.createCustomer(
                    buildJsonObject { put("email", CUSTOMER_EMAIL) },
                ).getOrThrow()
                automaticTaxSessionParams(customerId, paymentMethodSave = true)
            },
            configuration = configuration,
            setup = { controller ->
                contentPage.clickOnLpm("card")
                fillOutCardAndBillingDetails("4242424242424242", CALIFORNIA_ADDRESS, "California")
                paymentSheetPage.assertSaveForFutureCheckboxNotChecked()
                paymentSheetPage.checkSaveForFuture()
                paymentSheetPage.assertSaveForFutureUseCheckboxChecked()
                formPage.clickPrimaryButtonWithoutWaitingForDismissal()

                val session = awaitBillingTaxSession(
                    controller = controller,
                    address = CALIFORNIA_ADDRESS,
                    expectPositiveTax = true,
                )
                formPage.waitUntilMissing()
                contentPage.assertHasSelectedLpm("card")
                assertCaliforniaTax(session)
            },
        )
        assertThat(saveResult).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)

        val reuseResult = runCheckoutIntegrationTest(
            merchant = TAX_MERCHANT,
            sessionParams = { automaticTaxSessionParams(customerId, paymentMethodSave = false) },
            configuration = configuration,
            setup = { controller ->
                contentPage.assertHasSelectedSavedCard("4242")
                val session = awaitBillingTaxSession(
                    controller = controller,
                    address = CALIFORNIA_ADDRESS,
                    expectPositiveTax = true,
                )
                assertCaliforniaTax(session)
            },
        )
        assertThat(reuseResult).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    @Test
    fun testChangingNewCardBillingAddressUpdatesTaxAndCompletesPayment() = runBlocking {
        val result = runCheckoutIntegrationTest(
            merchant = TAX_MERCHANT,
            sessionParams = { backend ->
                val customerId = backend.createCustomer(
                    buildJsonObject { put("email", CUSTOMER_EMAIL) },
                ).getOrThrow()
                automaticTaxSessionParams(customerId, paymentMethodSave = false)
            },
            configuration = configuration,
            setup = { controller ->
                contentPage.clickOnLpm("card")
                fillOutCardAndBillingDetails("4242424242424242", CALIFORNIA_ADDRESS, "California")
                formPage.clickPrimaryButtonWithoutWaitingForDismissal()

                val californiaSession = awaitBillingTaxSession(
                    controller = controller,
                    address = CALIFORNIA_ADDRESS,
                    expectPositiveTax = true,
                )
                formPage.waitUntilMissing()
                contentPage.assertHasSelectedLpm("card")
                assertCaliforniaTax(californiaSession)

                contentPage.clickOnLpm("card")
                fillOutCardAndBillingDetails("5555555555554444", OREGON_ADDRESS, "Oregon")
                formPage.clickPrimaryButtonWithoutWaitingForDismissal()

                val oregonSession = awaitBillingTaxSession(
                    controller = controller,
                    address = OREGON_ADDRESS,
                    expectPositiveTax = false,
                )
                formPage.waitUntilMissing()
                contentPage.assertHasSelectedLpm("card")
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
            sessionParams = { cardOnlySessionParams() },
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

    private fun fillOutCardAndBillingDetails(
        cardNumber: String,
        address: CheckoutElementsController.Address.State,
        stateName: String,
    ) {
        formPage.fillOutCardDetails(newCardNumber = cardNumber)
        billingDetailsPage.fillOutUnitedStatesAddress(
            line1 = checkNotNull(address.line1),
            city = checkNotNull(address.city),
            stateName = stateName,
            zipCode = checkNotNull(address.postalCode),
        )
    }

    private suspend fun awaitBillingTaxSession(
        controller: CheckoutElementsController,
        address: CheckoutElementsController.Address.State,
        expectPositiveTax: Boolean,
    ): CheckoutElementsController.Session {
        val session = withTimeout(30.seconds) {
            combine(controller.session, controller.isUpdating) { session, isUpdating ->
                session?.takeIf {
                    val tax = it.totals.taxExclusive.minorUnitsAmount
                    !isUpdating &&
                        it.paymentOption?.billingDetails?.address?.state == address.state &&
                        it.tax?.status == CheckoutElementsController.Session.Tax.Status.Ready &&
                        (if (expectPositiveTax) tax > 0L else tax == 0L)
                }
            }.first { it != null }
        }
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

    private fun assertCaliforniaTax(session: CheckoutElementsController.Session) {
        val totals = session.totals
        assertThat(totals.subtotal.minorUnitsAmount).isEqualTo(SUBTOTAL)
        assertThat(totals.taxExclusive.minorUnitsAmount).isGreaterThan(0L)
        assertThat(totals.total.minorUnitsAmount).isEqualTo(
            totals.subtotal.minorUnitsAmount + totals.taxExclusive.minorUnitsAmount,
        )
    }

    private fun automaticTaxSessionParams(customerId: String, paymentMethodSave: Boolean): JsonObject {
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
                    put("payment_method_save", if (paymentMethodSave) "enabled" else "disabled")
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

        val CALIFORNIA_ADDRESS = CheckoutElementsController.Address.State(
            line1 = "123 Main St",
            line2 = null,
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US",
        )
        val OREGON_ADDRESS = CheckoutElementsController.Address.State(
            line1 = "123 Main St",
            line2 = null,
            city = "Portland",
            state = "OR",
            postalCode = "97201",
            country = "US",
        )
    }
}
