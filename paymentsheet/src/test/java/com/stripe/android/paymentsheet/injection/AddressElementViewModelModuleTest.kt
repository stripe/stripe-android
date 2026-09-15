package com.stripe.android.paymentsheet.injection

import android.content.Context
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AddressElementViewModelModuleTest {
    private val module = AddressElementViewModelModule()

    @Test
    fun `provideInlinePlacesClient returns hosted client by default when google client is available`() {
        val googlePlacesClient = mock<PlacesClientProxy>()
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(),
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            googlePlacesClient = googlePlacesClient,
            addressLauncherEventReporter = FakeAddressLauncherEventReporter(),
        )

        assertThat(placesClient).isInstanceOf(StripeHostedPlacesClientProxy::class.java)
        assertThat(placesClient).isNotSameInstanceAs(googlePlacesClient)
    }

    @Test
    fun `provideGooglePlacesClient returns null without google api key`() {
        val placesClient = module.provideGooglePlacesClient(
            context = mock<Context>(),
            args = AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(billingAddress = null),
            ),
            apiConfigurationProvider = { DEFAULT_API_CONFIG },
        )

        assertThat(placesClient).isNull()
    }

    @Test
    fun `provideStripeAutocompleteRepository uses the provided request options`() = runTest {
        val networkClient = FakeStripeNetworkClient()
        val repository = module.provideStripeAutocompleteRepository(
            stripeNetworkClient = networkClient,
            requestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        val result = repository.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            sessionToken = "session_123",
            locale = null,
        )

        assertThat(result.isSuccess).isTrue()
        val request = networkClient.requests.awaitItem() as ApiRequest
        assertThat(request.options.apiKey).isEqualTo(DEFAULT_API_CONFIG.publishableKey)
        assertThat(request.options.stripeAccount).isEqualTo(DEFAULT_API_CONFIG.stripeAccountId)
        networkClient.requests.ensureAllEventsConsumed()
    }

    internal class FakeStripeNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return StripeResponse(code = 200, body = """{"suggestions":[]}""")
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File,
        ): StripeResponse<File> = error("File requests are not expected")
    }
}
