package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentElementAutocompleteAddressInteractorTest {
    @Test
    fun `autocompleteConfig property returns provided config`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US", "GB")
        )
        val interactor = createInteractor(autocompleteConfig = config)

        assertThat(interactor.autocompleteConfig).isEqualTo(config)
        assertThat(interactor.autocompleteConfig.autocompleteCountries).containsExactly("US", "GB")
    }

    @Test
    fun `Factory creates PaymentElementAutocompleteAddressInteractor`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US")
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig).isEqualTo(config)
    }

    @Test
    fun `Factory falls back to no-autocomplete interactor when proxy flag is off`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )
        val repository = FakeStripeAutocompleteRepository()

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = repository,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `Factory disposes previously created inline interactor on next create`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )
        val repository = FakeStripeAutocompleteRepository()
        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = repository,
            coroutineScope = backgroundScope,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
            eventReporter = FakeAddressLauncherEventReporter(),
        )
        val queryFlow = MutableStateFlow("")
        val countryFlow = MutableStateFlow<String?>("US")

        val first = factory.create()
        first.observeQueryChanges(queryFlow, countryFlow)
        first.onFocusGained()

        // Creating a new interactor disposes the previous one, so its observation is cancelled.
        factory.create()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        // No predictions are fetched because the first interactor was disposed.
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `Factory creates inline interactor when proxy flag is on with repository`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
    }

    @Test
    fun `Factory falls back to no-autocomplete interactor when repository is null`() = test {
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
    }

    private fun test(test: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) {
        test()
    }

    private fun createInteractor(
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US", "CA")
        ),
    ) = PaymentElementAutocompleteAddressInteractor(
        autocompleteConfig = autocompleteConfig,
    )
}
