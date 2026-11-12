package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentElementAutocompleteAddressInteractorTest {
    @Test
    fun `onAutocomplete is a no-op`() {
        val interactor = createInteractor()

        // Should not throw and should not emit any event.
        var emitted = false
        interactor.register { emitted = true }

        interactor.onAutocomplete("US")

        assertThat(emitted).isFalse()
    }

    @Test
    fun `autocompleteConfig property returns provided config`() {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US", "GB")
        )
        val interactor = createInteractor(autocompleteConfig = config)

        assertThat(interactor.autocompleteConfig).isEqualTo(config)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).containsExactly("US", "GB")
    }

    @Test
    fun `Factory creates disabled interactor when there is no coroutine scope`() = runTest {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US")
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = null,
            coroutineScope = null,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig).isEqualTo(config)
    }

    @Test
    fun `Factory creates inline interactor when stripe hosted is enabled with a repository`() = runTest {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
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
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).isEqualTo(setOf("US"))
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
    }

    @Test
    fun `Factory disposes previously created inline interactor on next create`() = runTest {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )
        val fakeRepository = FakeStripeAutocompleteRepository()
        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = fakeRepository,
            coroutineScope = backgroundScope,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
            eventReporter = FakeAddressLauncherEventReporter(),
        )
        val queryFlow = MutableStateFlow("")
        val countryFlow = MutableStateFlow<String?>("US")

        val first = factory.create()
        first.observeQueryChanges(queryFlow, countryFlow)

        // Creating a new interactor disposes the previous one, so its observation is cancelled.
        factory.create()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        // No predictions are fetched because the first interactor was disposed.
        fakeRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `Factory creates disabled interactor when stripe hosted is off`() = runTest {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
    }

    @Test
    fun `Factory creates disabled interactor when stripe hosted is on but repository is null`() = runTest {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
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

    @Test
    fun `Factory creates disabled interactor when inline autocomplete is disabled`() = runTest {
        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "test-key",
                autocompleteCountries = setOf("US"),
                isInlineAutocompleteEnabled = false,
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)

        // The disabled interactor's onAutocomplete remains a no-op.
        var emitted = false
        interactor.register { emitted = true }
        interactor.onAutocomplete("US")

        assertThat(emitted).isFalse()
    }

    private fun createInteractor(
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-api-key",
            autocompleteCountries = setOf("US", "CA")
        ),
    ) = PaymentElementAutocompleteAddressInteractor(
        autocompleteConfig = autocompleteConfig,
    )
}
