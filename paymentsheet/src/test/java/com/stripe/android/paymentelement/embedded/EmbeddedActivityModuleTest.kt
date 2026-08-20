package com.stripe.android.paymentelement.embedded

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.BillingInlineAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class EmbeddedActivityModuleTest {

    @Test
    fun `autocomplete factory uses Stripe-hosted inline interactor when enabled`() = runScenario(
        shouldUseAutocompleteProxyEndpoints = true,
    ) {
        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isNull()
        assertThat(interactor.autocompleteConfig.autocompleteCountries)
            .isEqualTo(AUTOCOMPLETE_DEFAULT_COUNTRIES)
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
    }

    @Test
    fun `autocomplete factory falls back to manual interactor when Stripe-hosted autocomplete is disabled`() =
        runScenario(
            shouldUseAutocompleteProxyEndpoints = false,
        ) {
            assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
            assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isNull()
            assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isFalse()
        }

    private fun runScenario(
        shouldUseAutocompleteProxyEndpoints: Boolean,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val factory = EmbeddedActivityModule.provideAutocompleteAddressInteractorFactory(
            launcher = TestAutocompleteLauncher.noOp(),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            placesClient = null,
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                shouldUseAutocompleteProxyEndpoints = shouldUseAutocompleteProxyEndpoints,
            ),
            eventReporter = eventReporter,
        )

        Scenario(factory.create()).block()

        eventReporter.validate()
    }

    private data class Scenario(
        val interactor: AutocompleteAddressInteractor,
    )
}
