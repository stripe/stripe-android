package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentElementAutocompleteAddressInteractorTest {
    @Test
    fun `onAutocomplete launches when googlePlacesApiKey is present`() = test { scenario ->
        val interactor = createInteractor(
            launcher = scenario.launcher,
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "test-api-key",
                autocompleteCountries = setOf("US", "CA")
            )
        )

        interactor.onAutocomplete("US")

        scenario.launchCalls.expectMostRecentItem().let { call ->
            assertThat(call.country).isEqualTo("US")
            assertThat(call.googlePlacesApiKey).isEqualTo("test-api-key")
        }
    }

    @Test
    fun `onAutocomplete does nothing when googlePlacesApiKey is null`() = test { scenario ->
        val interactor = createInteractor(
            launcher = scenario.launcher,
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = null,
                autocompleteCountries = setOf("US", "CA")
            )
        )

        interactor.onAutocomplete("US")

        scenario.launchCalls.expectNoEvents()
    }

    @Test
    fun `emits 'OnExpandForm' event when launcher result is EnterManually`() = test { scenario ->
        val interactor = createInteractor(launcher = scenario.launcher)
        val deferredEvent = CompletableDeferred<AutocompleteAddressInteractor.Event>()

        interactor.register {
            deferredEvent.complete(it)
        }

        interactor.onAutocomplete("US")

        val launchCall = scenario.launchCalls.awaitItem()

        val address = createTestAddress()
        val result = AutocompleteLauncher.Result.EnterManually(address)

        launchCall.resultHandler.onAutocompleteLauncherResult(result)

        val event = deferredEvent.await()

        assertThat(event).isInstanceOf(AutocompleteAddressInteractor.Event.OnExpandForm::class.java)

        val expandFormEvent = event as AutocompleteAddressInteractor.Event.OnExpandForm

        assertThat(expandFormEvent.values).isNotNull()
        assertThat(expandFormEvent.values).containsExactlyEntriesIn(
            mapOf(
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "Apt 4B",
                IdentifierSpec.City to "San Francisco",
                IdentifierSpec.State to "CA",
                IdentifierSpec.PostalCode to "94105",
                IdentifierSpec.Country to "US",
            )
        )
    }

    @Test
    fun `emits OnValues event when launcher result is OnBack with address details`() = test { scenario ->
        val interactor = createInteractor(launcher = scenario.launcher)
        val deferredEvent = CompletableDeferred<AutocompleteAddressInteractor.Event>()

        interactor.register {
            deferredEvent.complete(it)
        }

        interactor.onAutocomplete("US")

        val launchCall = scenario.launchCalls.awaitItem()

        val address = createTestAddress()
        val result = AutocompleteLauncher.Result.OnBack(address)

        launchCall.resultHandler.onAutocompleteLauncherResult(result)

        val event = deferredEvent.await()

        assertThat(event).isInstanceOf(AutocompleteAddressInteractor.Event.OnValues::class.java)

        val valuesEvent = event as AutocompleteAddressInteractor.Event.OnValues

        assertThat(valuesEvent.values).containsExactlyEntriesIn(
            mapOf(
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "Apt 4B",
                IdentifierSpec.City to "San Francisco",
                IdentifierSpec.State to "CA",
                IdentifierSpec.PostalCode to "94105",
                IdentifierSpec.Country to "US",
            )
        )
    }

    @Test
    fun `does not emit event when launcher result is OnBack with null address details`() = test { scenario ->
        val interactor = createInteractor(launcher = scenario.launcher)
        val deferredEvent = CompletableDeferred<AutocompleteAddressInteractor.Event>()

        interactor.register {
            deferredEvent.complete(it)
        }

        interactor.onAutocomplete("US")

        val launchCall = scenario.launchCalls.awaitItem()
        val result = AutocompleteLauncher.Result.OnBack(null)

        launchCall.resultHandler.onAutocompleteLauncherResult(result)

        assertThat(deferredEvent.isActive).isTrue()
    }

    @Test
    fun `emits OnExpandForm event when launcher result is EnterManually with null address`() = test { scenario ->
        val interactor = createInteractor(launcher = scenario.launcher)
        val deferredEvent = CompletableDeferred<AutocompleteAddressInteractor.Event>()

        interactor.register {
            deferredEvent.complete(it)
        }

        interactor.onAutocomplete("US")

        val launchCall = scenario.launchCalls.awaitItem()
        val result = AutocompleteLauncher.Result.EnterManually(null)

        launchCall.resultHandler.onAutocompleteLauncherResult(result)

        val event = deferredEvent.await()

        assertThat(event).isInstanceOf(AutocompleteAddressInteractor.Event.OnExpandForm::class.java)

        val expandFormEvent = event as AutocompleteAddressInteractor.Event.OnExpandForm

        assertThat(expandFormEvent.values).isNull()
    }

    @Test
    fun `autocompleteConfig property returns provided config`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US", "GB")
        )
        val interactor = createInteractor(launcher = scenario.launcher, autocompleteConfig = config)

        assertThat(interactor.autocompleteConfig).isEqualTo(config)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).containsExactly("US", "GB")
    }

    @Test
    fun `Factory creates PaymentElementAutocompleteAddressInteractor`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US")
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = null,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig).isEqualTo(config)
    }

    @Test
    fun `Factory creates inline interactor when inline enabled with places client and scope`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = FakePlacesClientProxy(),
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("test-key")
        assertThat(interactor.autocompleteConfig.autocompleteCountries).isEqualTo(setOf("US"))
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isFalse()
    }

    @Test
    fun `Factory disposes previously created inline interactor on next create`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
        )
        val fakePlaces = FakePlacesClientProxy()
        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = fakePlaces,
            stripeAutocompleteRepository = null,
            coroutineScope = backgroundScope,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
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
        fakePlaces.ensureAllEventsConsumed()
    }

    @Test
    fun `Factory falls back to launcher when inline enabled but placesClient is null`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-key",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = null,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
    }

    @Test
    fun `Factory creates inline interactor when proxy flag is on with repository`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = false,
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = null,
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
        assertThat(interactor.autocompleteConfig.isPlacesAvailable).isFalse()
    }

    @Test
    fun `Factory falls back to launcher when proxy flag is on but repository is null`() = test { scenario ->
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = false,
            isInlineAutocompleteEnabled = true,
        )

        val factory = PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = scenario.launcher,
            autocompleteConfig = config,
            placesClient = null,
            stripeAutocompleteRepository = null,
            coroutineScope = this,
            shouldUseAutocompleteProxyEndpointsProvider = { true },
        )

        val interactor = factory.create()

        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
    }

    @Test
    fun `multiple onAutocomplete calls with different countries`() = test { scenario ->
        val interactor = createInteractor(launcher = scenario.launcher)

        interactor.onAutocomplete("US")
        interactor.onAutocomplete("CA")
        interactor.onAutocomplete("GB")

        val usCall = scenario.launchCalls.awaitItem()

        assertThat(usCall.country).isEqualTo("US")
        assertThat(usCall.googlePlacesApiKey).isEqualTo("test-api-key")

        val caCall = scenario.launchCalls.awaitItem()

        assertThat(caCall.country).isEqualTo("CA")
        assertThat(caCall.googlePlacesApiKey).isEqualTo("test-api-key")

        val gbCall = scenario.launchCalls.awaitItem()

        assertThat(gbCall.country).isEqualTo("GB")
        assertThat(gbCall.googlePlacesApiKey).isEqualTo("test-api-key")
    }

    private fun test(test: suspend TestScope.(TestAutocompleteLauncher.Scenario) -> Unit) = runTest(
        UnconfinedTestDispatcher()
    ) {
        TestAutocompleteLauncher.test {
            test(this)
        }
    }

    private fun createInteractor(
        launcher: AutocompleteLauncher = TestAutocompleteLauncher.noOp(),
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test-api-key",
            autocompleteCountries = setOf("US", "CA")
        ),
    ) = PaymentElementAutocompleteAddressInteractor(
        launcher = launcher,
        autocompleteConfig = autocompleteConfig,
    )

    private fun createTestAddress() = PaymentSheet.Address(
        line1 = "123 Main Street",
        line2 = "Apt 4B",
        city = "San Francisco",
        state = "CA",
        postalCode = "94105",
        country = "US"
    )
}
