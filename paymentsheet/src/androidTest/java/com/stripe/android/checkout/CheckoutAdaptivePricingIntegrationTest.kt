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
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
    fun testSwitchingToIntegrationCurrencyUpdatesSessionAndPays() = runScenario { controller ->
        assertPresentsLocalCurrency(controller.session.value)

        currencySelector.clickCurrencyOption("USD")

        // The session reflects the new currency only after the update response, not when the option is tapped.
        val session = controller.awaitSession("USD presentment") {
            it.presentmentDetails?.presentmentCurrency == INTEGRATION_CURRENCY
        }
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(SUBTOTAL)
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
        assertThat(session?.currency).isEqualTo(INTEGRATION_CURRENCY)
        assertThat(session?.presentmentDetails?.presentmentCurrency).isEqualTo(LOCAL_CURRENCY)
        currencySelector.assertCurrencyOptionIsSelected("EUR")
    }

    private companion object {
        // Stripe test mode simulates a French customer from the +location_FR email suffix.
        const val FRANCE_CUSTOMER_EMAIL = "test+location_FR@example.com"
        const val INTEGRATION_CURRENCY = "usd"
        const val LOCAL_CURRENCY = "eur"
        const val SUBTOTAL = 1000L
    }
}
