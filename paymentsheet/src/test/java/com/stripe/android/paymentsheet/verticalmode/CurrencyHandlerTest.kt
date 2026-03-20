package com.stripe.android.paymentsheet.verticalmode

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.InternalState
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.AdaptivePricingInfo
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.LocalCurrencyOption
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CurrencyHandlerTest {

    private val applicationContext: Application
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `currencySelectorOptions null when adaptivePricingInfo is null`() = runScenario(
        adaptivePricingInfo = null,
    ) {
        assertThat(handler.currencySelectorOptions).isNull()
    }

    @Test
    fun `currencySelectorOptions built from adaptivePricingInfo`() = runScenario(
        adaptivePricingInfo = ADAPTIVE_PRICING_INFO,
    ) {
        val options = handler.currencySelectorOptions
        assertThat(options).isNotNull()
        assertThat(options!!.first.code).isEqualTo("usd")
        assertThat(options.first.displayableText).isNotEmpty()
        assertThat(options.second.code).isEqualTo("eur")
        assertThat(options.second.displayableText).isNotEmpty()
        assertThat(options.selectedCode).isEqualTo("eur")
        assertThat(options.exchangeRateText).isEqualTo("1 USD = 0.9 EUR")
    }

    @Test
    fun `selectedCurrency matches activePresentmentCurrency`() = runScenario(
        adaptivePricingInfo = ADAPTIVE_PRICING_INFO,
    ) {
        assertThat(handler.currencySelectorOptions?.selectedCode).isEqualTo("eur")
    }

    @Test
    fun `selectedCurrency falls back to first when active not found`() = runScenario(
        adaptivePricingInfo = ADAPTIVE_PRICING_INFO.copy(activePresentmentCurrency = "jpy"),
    ) {
        assertThat(handler.currencySelectorOptions?.selectedCode).isEqualTo("usd")
    }

    @Test
    fun `exchangeRateText null when integration currency is active`() = runScenario(
        adaptivePricingInfo = ADAPTIVE_PRICING_INFO.copy(activePresentmentCurrency = "usd"),
    ) {
        assertThat(handler.currencySelectorOptions?.exchangeRateText).isNull()
    }

    @Test
    fun `onCurrencySelected invokes callback`() = runScenario(
        adaptivePricingInfo = ADAPTIVE_PRICING_INFO,
    ) {
        handler.onCurrencySelected(CurrencyOption(code = "usd", displayableText = ""))

        assertThat(onCurrencyChangedTurbine.awaitItem()).isEqualTo("usd")
    }

    @Test
    fun `create returns handler with currency options when checkout session`() {
        val instancesKey = "test_key"
        val checkoutSessionResponse = createCheckoutSessionResponse(ADAPTIVE_PRICING_INFO)
        val internalState = InternalState(
            key = instancesKey,
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val checkout = Checkout.createWithState(applicationContext, Checkout.State(internalState))
        CheckoutInstances.add(instancesKey, checkout)

        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_123",
                instancesKey = instancesKey,
            ),
        )

        val handler = CurrencyHandler.create(metadata)
        val options = handler.currencySelectorOptions

        assertThat(options).isNotNull()
        assertThat(options!!.first.code).isEqualTo("usd")
        assertThat(options.second.code).isEqualTo("eur")
        assertThat(options.selectedCode).isEqualTo("eur")
    }

    @Test
    fun `create returns empty handler when not checkout session`() {
        val metadata = PaymentMethodMetadataFactory.create()

        val handler = CurrencyHandler.create(metadata)

        assertThat(handler.currencySelectorOptions).isNull()
    }

    private fun runScenario(
        adaptivePricingInfo: AdaptivePricingInfo?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onCurrencyChangedTurbine = Turbine<String>()
        val handler = CurrencyHandler(
            adaptivePricingInfo = adaptivePricingInfo,
            onCurrencyChanged = { onCurrencyChangedTurbine.add(it) },
        )
        block(
            Scenario(
                handler = handler,
                onCurrencyChangedTurbine = onCurrencyChangedTurbine,
            )
        )
        onCurrencyChangedTurbine.ensureAllEventsConsumed()
    }

    private class Scenario(
        val handler: CurrencyHandler,
        val onCurrencyChangedTurbine: Turbine<String>,
    )

    companion object {
        private val ADAPTIVE_PRICING_INFO = AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 1000,
            integrationCurrency = "usd",
            localCurrencyOptions = listOf(
                LocalCurrencyOption(
                    amount = 900,
                    conversionMarkupBps = 200,
                    currency = "eur",
                    presentmentExchangeRate = "0.9",
                ),
                LocalCurrencyOption(
                    amount = 800,
                    conversionMarkupBps = 200,
                    currency = "gbp",
                    presentmentExchangeRate = "0.8",
                ),
            ),
        )

        private fun createCheckoutSessionResponse(
            adaptivePricingInfo: AdaptivePricingInfo,
        ): CheckoutSessionResponse {
            return CheckoutSessionResponse(
                id = "cs_123",
                amount = 1000L,
                currency = "usd",
                mode = CheckoutSessionResponse.Mode.PAYMENT,
                customerEmail = null,
                elementsSession = null,
                paymentIntent = null,
                setupIntent = null,
                customer = null,
                savedPaymentMethodsOfferSave = null,
                totalSummary = null,
                lineItems = emptyList(),
                shippingOptions = emptyList(),
                adaptivePricingInfo = adaptivePricingInfo,
            )
        }
    }
}
