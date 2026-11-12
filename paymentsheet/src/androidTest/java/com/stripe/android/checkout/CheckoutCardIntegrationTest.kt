package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.paymentelementtestpages.BillingDetailsPage
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
    fun testSuccessfulCardPayment() = runPlainScenario(
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
    fun testDeclinedCardReportsFailure() = runPlainScenario(
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
    fun testCardWith3ds2ChallengeCompletes() = runPlainScenario(
        setup = {
            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails(newCardNumber = "4000000000003220")
            formPage.clickPrimaryButton()
        },
        handleNextAction = {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val completeButton = device.wait(
                Until.findObject(By.text("COMPLETE")),
                CHALLENGE_TIMEOUT.inWholeMilliseconds,
            )
            checkNotNull(completeButton) { "3DS2 challenge COMPLETE button not found" }.click()
        },
        check = { result ->
            assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
        },
    )

    @Test
    fun testSavedCardIsReusedWithBillingTaxInAnotherSession() = runTaxScenario {
        runSession(paymentMethodSave = true) { controller ->
            assertAwaitingBillingAddress(controller.session.value)

            contentPage.clickOnLpm("card")
            billingDetailsPage.enterAddressManually(checkNotNull(CALIFORNIA_ADDRESS.line1))
            fillOutCardAndBillingDetails("4242424242424242", CALIFORNIA_ADDRESS, "California")
            paymentSheetPage.assertSaveForFutureCheckboxNotChecked()
            paymentSheetPage.checkSaveForFuture()
            paymentSheetPage.assertSaveForFutureUseCheckboxChecked()
            formPage.clickPrimaryButtonWithoutWaitingForDismissal()

            val session = awaitSession(controller, "California tax") {
                it.tax?.status == CheckoutElementsController.Session.Tax.Status.Ready
            }
            formPage.waitUntilMissing()
            contentPage.assertHasSelectedLpm("card")
            assertBillingAddress(session, CALIFORNIA_ADDRESS)
            assertTaxed(session)
        }

        runSession(paymentMethodSave = false) { controller ->
            contentPage.assertHasSelectedSavedCard("4242")
            // configure publishes the session only after its billing tax update, so no wait is needed.
            val session = checkNotNull(controller.session.value)
            assertBillingAddress(session, CALIFORNIA_ADDRESS)
            assertTaxed(session)
        }
    }

    @Test
    fun testChangingNewCardBillingAddressUpdatesTaxAndCompletesPayment() = runTaxScenario {
        runSession(paymentMethodSave = false) { controller ->
            assertAwaitingBillingAddress(controller.session.value)

            contentPage.clickOnLpm("card")
            billingDetailsPage.enterAddressManually(checkNotNull(CALIFORNIA_ADDRESS.line1))
            fillOutCardAndBillingDetails("4242424242424242", CALIFORNIA_ADDRESS, "California")
            formPage.clickPrimaryButtonWithoutWaitingForDismissal()

            val californiaSession = awaitSession(controller, "California tax") {
                it.tax?.status == CheckoutElementsController.Session.Tax.Status.Ready
            }
            formPage.waitUntilMissing()
            contentPage.assertHasSelectedLpm("card")
            assertBillingAddress(californiaSession, CALIFORNIA_ADDRESS)
            assertTaxed(californiaSession)

            contentPage.clickOnLpm("card")
            fillOutCardAndBillingDetails("5555555555554444", OREGON_ADDRESS, "Oregon")
            formPage.clickPrimaryButtonWithoutWaitingForDismissal()

            val californiaTax = californiaSession.totals.taxExclusive.minorUnitsAmount
            val oregonSession = awaitSession(controller, "Oregon tax") {
                it.totals.taxExclusive.minorUnitsAmount != californiaTax
            }
            formPage.waitUntilMissing()
            contentPage.assertHasSelectedLpm("card")
            assertBillingAddress(oregonSession, OREGON_ADDRESS)
            assertUntaxed(oregonSession)
        }
    }

    private fun runPlainScenario(
        setup: suspend (CheckoutElementsController) -> Unit,
        handleNextAction: suspend () -> Unit = {},
        check: (CheckoutElementsController.Result) -> Unit,
    ) = runBlocking {
        val backend = CheckoutPlaygroundBackend(
            baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
            merchant = "US",
        )
        val result = runCheckoutIntegrationTest(
            backend = backend,
            sessionParams = cardOnlySessionParams(),
            configuration = configuration,
            setup = setup,
            handleNextAction = handleNextAction,
        )
        check(result)
    }

    private fun runTaxScenario(block: suspend TaxScenario.() -> Unit) = runBlocking {
        val backend = CheckoutPlaygroundBackend(
            baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
            merchant = TAX_MERCHANT,
        )
        val customerId = backend.createCustomer(
            buildJsonObject { put("email", CUSTOMER_EMAIL) },
        ).getOrThrow()
        TaxScenario(backend, customerId).block()
    }

    private inner class TaxScenario(
        private val backend: CheckoutPlaygroundBackend,
        private val customerId: String,
    ) {
        suspend fun runSession(
            paymentMethodSave: Boolean,
            setup: suspend (CheckoutElementsController) -> Unit,
        ) {
            val result = runCheckoutIntegrationTest(
                backend = backend,
                sessionParams = automaticTaxSessionParams(customerId, paymentMethodSave),
                configuration = configuration,
                setup = setup,
                handleNextAction = {},
            )
            assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
        }
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
        // Line1 is already in the inline-autocomplete "Address" field, from manual entry or the reopened selection.
        formPage.fillOutCardDetails(newCardNumber = cardNumber)
        billingDetailsPage.fillOutUnitedStatesCityStateAndZip(
            city = checkNotNull(address.city),
            stateName = stateName,
            zipCode = checkNotNull(address.postalCode),
        )
    }

    // The sheet result commits the new selection before the refreshed response, so wait on response-derived fields.
    private suspend fun awaitSession(
        controller: CheckoutElementsController,
        description: String,
        predicate: (CheckoutElementsController.Session) -> Boolean,
    ): CheckoutElementsController.Session {
        return withTimeoutOrNull(30.seconds) {
            controller.session.filterNotNull().first(predicate)
        } ?: error("Timed out waiting for $description. Last session: ${controller.session.value}")
    }

    private fun assertAwaitingBillingAddress(session: CheckoutElementsController.Session?) {
        assertThat(session?.tax?.status)
            .isEqualTo(CheckoutElementsController.Session.Tax.Status.RequiresBillingAddress)
        assertThat(session?.paymentOption?.billingDetails?.address).isNull()
    }

    private fun assertBillingAddress(
        session: CheckoutElementsController.Session,
        address: CheckoutElementsController.Address.State,
    ) {
        val selectedAddress = session.paymentOption?.billingDetails?.address
        assertThat(selectedAddress?.line1).isEqualTo(address.line1)
        assertThat(selectedAddress?.city).isEqualTo(address.city)
        assertThat(selectedAddress?.state).isEqualTo(address.state)
        assertThat(selectedAddress?.postalCode).isEqualTo(address.postalCode)
        assertThat(selectedAddress?.country).isEqualTo(address.country)
    }

    private fun assertTaxed(session: CheckoutElementsController.Session) {
        val totals = session.totals
        assertThat(session.tax?.status).isEqualTo(CheckoutElementsController.Session.Tax.Status.Ready)
        assertThat(totals.subtotal.minorUnitsAmount).isEqualTo(SUBTOTAL)
        assertThat(totals.taxExclusive.minorUnitsAmount).isGreaterThan(0L)
        assertThat(totals.total.minorUnitsAmount).isEqualTo(
            totals.subtotal.minorUnitsAmount + totals.taxExclusive.minorUnitsAmount,
        )
    }

    private fun assertUntaxed(session: CheckoutElementsController.Session) {
        val totals = session.totals
        assertThat(session.tax?.status).isEqualTo(CheckoutElementsController.Session.Tax.Status.Ready)
        assertThat(totals.subtotal.minorUnitsAmount).isEqualTo(SUBTOTAL)
        assertThat(totals.taxExclusive.minorUnitsAmount).isEqualTo(0L)
        assertThat(totals.total.minorUnitsAmount).isEqualTo(SUBTOTAL)
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
        val CHALLENGE_TIMEOUT = 30.seconds

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
