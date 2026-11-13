package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
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
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US", "GB")
        )
        val interactor = createInteractor(autocompleteConfig = config)

        assertThat(interactor.autocompleteConfig).isEqualTo(config)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).containsExactly("US", "GB")
    }

    @Test
    fun `Factory creates PaymentElementAutocompleteAddressInteractor`() = test {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US")
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = null,
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
    fun `Factory creates inline interactor when inline enabled with places client and scope`() = test {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = FakePlacesClientProxy(
                findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
                fetchPlaceResult = Result.success(Address()),
            ),
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).isEqualTo(setOf("US"))
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isFalse()
    }

    @Test
    fun `Factory disposes previously created inline interactor on next create`() = test {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )
        val fakePlaces = FakePlacesClientProxy(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = fakePlaces,
            stripeAutocompleteRepository = null,
            coroutineScope = backgroundScope,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
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
        fakePlaces.ensureAllEventsConsumed()
    }

    @Test
    fun `Factory falls back to no-autocomplete interactor when placesClient is null`() = test {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = null,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = FakeAddressLauncherEventReporter(),
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
    }

    @Test
    fun `Factory creates inline interactor when proxy flag is on with repository`() = test {
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = null,
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
            googlePlacesApiKey = null,
            autocompleteCountries = setOf("US"),
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            autocompleteConfig = config,
            placesClient = null,
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
            googlePlacesApiKey = "test-api-key",
            autocompleteCountries = setOf("US", "CA")
        ),
    ) = PaymentElementAutocompleteAddressInteractor(
        autocompleteConfig = autocompleteConfig,
    )
}
