package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.paymentelementtestpages.CurrencySelector
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutAdaptivePricingIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contentPage = EmbeddedContentPage(composeRule)
    private val formPage = EmbeddedFormPage(composeRule)
    private val currencySelector = CurrencySelector(composeRule)

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
        .currencySelectorElement(CurrencySelectorElement.Configuration())

    @Test
    fun testPaysInLocalCurrency() = runScenario { controller ->
        assertPresentsLocalCurrency(controller.session.value)

        payWithCard()
    }

    @Test
    fun testSwitchingToSettlementCurrencyUpdatesSessionAndPays() = runScenario { controller ->
        assertPresentsLocalCurrency(controller.session.value)

        currencySelector.clickCurrencyOption("USD")

        val session = awaitSession(controller, "USD presentment") {
            it.presentmentDetails?.presentmentCurrency == SETTLEMENT_CURRENCY &&
                it.totals.total.minorUnitsAmount == SUBTOTAL
        }
        assertThat(session.currency).isEqualTo(SETTLEMENT_CURRENCY)
        currencySelector.assertCurrencyOptionIsSelected("USD")

        payWithCard()
    }

    private fun runScenario(
        setup: suspend (CheckoutElementsController) -> Unit,
    ) = runBlocking {
        val backend = CheckoutPlaygroundBackend(
            baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
            merchant = "US",
        )
        val result = runCheckoutIntegrationTest(
            backend = backend,
            sessionParams = CheckoutSessionParamsBuilder(unitAmount = SUBTOTAL)
                .customerEmail(FRANCE_CUSTOMER_EMAIL)
                .build(),
            configuration = configuration,
            setup = setup,
            handleNextAction = {},
        )
        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    private fun payWithCard() {
        contentPage.clickOnLpm("card")
        formPage.fillOutCardDetails(newCardNumber = "4242424242424242")
        formPage.clickPrimaryButton()
    }

    private fun assertPresentsLocalCurrency(session: CheckoutElementsController.Session?) {
        assertThat(session?.currency).isEqualTo(SETTLEMENT_CURRENCY)
        assertThat(session?.presentmentDetails?.presentmentCurrency).isEqualTo(LOCAL_CURRENCY)
        currencySelector.assertCurrencyOptionIsSelected("EUR")
    }

    // The session only reflects the new currency after the update response, not when the option is tapped.
    private suspend fun awaitSession(
        controller: CheckoutElementsController,
        description: String,
        predicate: (CheckoutElementsController.Session) -> Boolean,
    ): CheckoutElementsController.Session {
        return withTimeoutOrNull(30.seconds) {
            controller.session.filterNotNull().first(predicate)
        } ?: error("Timed out waiting for $description. Last session: ${controller.session.value}")
    }

    private companion object {
        // The playground backend geolocates the customer from this email.
        const val FRANCE_CUSTOMER_EMAIL = "test+location_FR@example.com"
        const val SETTLEMENT_CURRENCY = "usd"
        const val LOCAL_CURRENCY = "eur"
        const val SUBTOTAL = 1000L
    }
}
